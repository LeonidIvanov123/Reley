package com.reley.messenger.dialog.service;

import com.reley.messenger.dialog.dto.DialogListItem;
import com.reley.messenger.dialog.dto.DialogResponse;
import com.reley.messenger.dialog.entity.Dialog;
import com.reley.messenger.dialog.repository.DialogParticipantRepository;
import com.reley.messenger.dialog.repository.DialogRepository;
import com.reley.messenger.exception.ChatHaveNoAccess;
import com.reley.messenger.exception.CreateDialogException;
import com.reley.messenger.message.service.MessageReaderService;
import com.reley.messenger.user.entity.User;
import com.reley.messenger.user.service.HydrateUserInfoApi;
import com.reley.messenger.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DialogServiceImpl implements DialogService {

    public final static String FAVORITE_DIALOG = "Избранное";

    private final HydrateUserInfoApi userServiceInfo;
    private final UserService userService;
    private final DialogRepository dialogRepository;
    private final DialogParticipantRepository participantRepository;
    private final MessageReaderService messageReaderService;
    private final DialogReadStateService dialogReadStateService;

    @Override
    public Mono<DialogResponse> createDialog(Long otherUserId) {

        return userServiceInfo.getCurrentUserId()
                .flatMap(currentUserId -> {

                    if (currentUserId.equals(otherUserId)) {
                        return createFavoritesDialog(currentUserId);
                    }

                    return userService.findById(otherUserId)
                            .switchIfEmpty(
                                    Mono.error(
                                            new CreateDialogException(
                                                    "Не найден юзер с id %s"
                                                            .formatted(otherUserId)
                                            )
                                    )
                            )
                            .flatMap(otherUser ->
                                    dialogRepository
                                            .findPrivateDialog(currentUserId, otherUser.getId())
                                            .map(dialog ->
                                                    new DialogResponse(
                                                            dialog.getId(),
                                                            Objects.equals(FAVORITE_DIALOG, dialog.getDialogName())
                                                                    ? FAVORITE_DIALOG
                                                                    : null,
                                                            otherUser.getId(),
                                                            Optional.ofNullable(otherUser.getName()).orElse(otherUser.getEmail()),
                                                            dialog.getCreatedAt()
                                                    )
                                            )
                                            .switchIfEmpty(
                                                    createNewDialog(
                                                            currentUserId,
                                                            otherUser
                                                    )
                                            )
                            );
                });
    }

    @Override
    public Flux<DialogListItem> getMyDialogs() {

        return userServiceInfo.getCurrentUser()
                .flatMapMany(user ->
                        dialogRepository.findAllByOwnerIdOrParticipantIdAndGroupChatFalse(
                                        user.userId(),
                                        user.userId()
                                )
                                .flatMap(dialog -> {

                                    Long otherUserId;

                                    if (Objects.equals(FAVORITE_DIALOG, dialog.getDialogName())) {
                                        otherUserId = user.userId();
                                    } else if (Objects.equals(dialog.getOwnerId(), user.userId())) {
                                        otherUserId = dialog.getParticipantId();
                                    } else {
                                        otherUserId = dialog.getOwnerId();
                                    }

                                    return Mono.zip(
                                                    userService.findById(otherUserId),
                                                    messageReaderService.findLastMessageDateByDialogId(dialog.getId()),
                                                    dialogReadStateService.getUnreadCount(dialog.getId(), user.userId())
                                            )
                                            .map(tuple -> {
                                                User otherUser = tuple.getT1();
                                                OffsetDateTime lastMessageAt = tuple.getT2();

                                                return new DialogListItem(
                                                        dialog.getId(), dialog.getDialogName(), otherUser,
                                                        lastMessageAt.equals(OffsetDateTime.MIN)
                                                                ? null
                                                                : lastMessageAt,
                                                        tuple.getT3()
                                                );
                                            });
                                })
                                .sort(Comparator.comparing(
                                        DialogListItem::lastMessageAt,
                                        Comparator.nullsLast(Comparator.reverseOrder())
                                ))

                );
    }

    public Mono<DialogResponse> createFavoritesDialog(Long userId) {

        return participantRepository
                .findFavoritesDialogId(userId)
                .flatMap(dialogRepository::findById)
                .map(dialog ->
                        new DialogResponse(
                                dialog.getId(),
                                dialog.getDialogName(),
                                userId,
                                FAVORITE_DIALOG,
                                dialog.getCreatedAt()
                        )
                )
                .switchIfEmpty(
                        Mono.defer(() ->
                                createNewFavoritesDialog(userId)
                        )
                );
    }

    @Override
    public Mono<Dialog> findById(Long id) {
        return userServiceInfo.getCurrentUser()
                .flatMap(user ->
                        checkAccessAndReturnDialog(id, user.userId())
                                .flatMap(dialog -> {

                                    if (dialog.getDialogName() != null) {
                                        return Mono.just(dialog);
                                    }

                                    Long otherUserId;

                                    if (Objects.equals(dialog.getOwnerId(), user.userId())) {
                                        otherUserId = dialog.getParticipantId();
                                    } else {
                                        otherUserId = dialog.getOwnerId();
                                    }

                                    return userService.findById(otherUserId)
                                            .map(otherUser -> {

                                                String dialogName;

                                                if (otherUser.getName() != null
                                                        && !otherUser.getName().isBlank()) {
                                                    dialogName = otherUser.getName()
                                                            + " (" + otherUser.getEmail() + ")";
                                                } else {
                                                    dialogName = otherUser.getEmail();
                                                }

                                                dialog.setDialogName(dialogName);

                                                return dialog;
                                            });
                                })
                );
    }

    @Override
    public Mono<Dialog> checkAccessAndReturnDialog(Long dialogId, Long userId) {

        return dialogRepository.findById(dialogId)
                .switchIfEmpty(Mono.error(
                        new ChatHaveNoAccess(
                                "Диалог с id %s не найден".formatted(dialogId)
                        )
                ))
                .flatMap(dialog -> {

                    boolean isParticipant =
                            Objects.equals(userId, dialog.getOwnerId())
                                    || Objects.equals(userId, dialog.getParticipantId());

                    return isParticipant
                            ? Mono.just(dialog)
                            : Mono.error(
                            new ChatHaveNoAccess("Нет доступа к диалогу c id [%s]".formatted(dialogId)
                            ));
                });
    }

    private Mono<DialogResponse> createNewFavoritesDialog(Long userId) {

        OffsetDateTime now = OffsetDateTime.now();

        Dialog dialog = Dialog.builder()
                .dialogName(FAVORITE_DIALOG)
                .ownerId(userId)
                .groupChat(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return dialogRepository.save(dialog)
                .flatMap(savedDialog ->
                        participantRepository
                                .insert(
                                        savedDialog.getId(),
                                        userId,
                                        now
                                )
                                .thenReturn(
                                        new DialogResponse(
                                                savedDialog.getId(),
                                                FAVORITE_DIALOG,
                                                userId,
                                                FAVORITE_DIALOG,
                                                savedDialog.getCreatedAt()
                                        )
                                )
                );
    }

    private Mono<DialogResponse> createNewDialog(Long currentUserId, User otherUser) {

        OffsetDateTime now = OffsetDateTime.now();

        Dialog dialog = Dialog.builder()
                .dialogName(null)
                .ownerId(currentUserId)
                .participantId(otherUser.getId())
                .groupChat(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return dialogRepository.save(dialog)
                .flatMap(savedDialog ->
                        participantRepository
                                .insert(
                                        savedDialog.getId(),
                                        currentUserId,
                                        now
                                )
                                .then(
                                        participantRepository.insert(
                                                savedDialog.getId(),
                                                otherUser.getId(),
                                                now
                                        )
                                )
                                .thenReturn(
                                        new DialogResponse(
                                                savedDialog.getId(),
                                                savedDialog.getDialogName(),
                                                otherUser.getId(),
                                                otherUser.getEmail(),
                                                savedDialog.getCreatedAt()
                                        )
                                )
                );
    }
}