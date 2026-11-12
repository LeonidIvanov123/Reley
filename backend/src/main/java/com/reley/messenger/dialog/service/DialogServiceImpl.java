package com.reley.messenger.dialog.service;

import com.reley.messenger.dialog.dto.DialogListItem;
import com.reley.messenger.dialog.dto.DialogResponse;
import com.reley.messenger.dialog.dto.GroupParticipantResponse;
import com.reley.messenger.dialog.dto.GroupSettingsResponse;
import com.reley.messenger.dialog.entity.Dialog;
import com.reley.messenger.dialog.entity.DialogParticipant;
import com.reley.messenger.dialog.repository.DialogParticipantRepository;
import com.reley.messenger.dialog.repository.DialogRepository;
import com.reley.messenger.exception.ChatHaveNoAccess;
import com.reley.messenger.exception.CreateDialogException;
import com.reley.messenger.exception.UserNotFoundException;
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

    public static final String FAVORITE_DIALOG = "Избранное";

    private final HydrateUserInfoApi userServiceInfo;
    private final UserService userService;
    private final DialogRepository dialogRepository;
    private final DialogParticipantRepository participantRepository;
    private final MessageReaderService messageReaderService;

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
                                            .findPrivateDialog(
                                                    currentUserId,
                                                    otherUser.getId()
                                            )
                                            .map(dialog ->
                                                    new DialogResponse(
                                                            dialog.getId(),
                                                            Objects.equals(
                                                                    FAVORITE_DIALOG,
                                                                    dialog.getDialogName()
                                                            )
                                                                    ? FAVORITE_DIALOG
                                                                    : null,
                                                            otherUser.getId(),
                                                            Optional.ofNullable(
                                                                            otherUser.getName()
                                                                    )
                                                                    .orElse(
                                                                            otherUser.getEmail()
                                                                    ),
                                                            dialog.getCreatedAt(),
                                                            dialog.isGroupChat()
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
    public Mono<DialogResponse> createGroup(String name) {

        return userServiceInfo.getCurrentUserId()
                .flatMap(userId -> {

                    OffsetDateTime now = OffsetDateTime.now();

                    Dialog dialog = Dialog.builder()
                            .dialogName(name)
                            .ownerId(userId)
                            .groupChat(true)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();

                    return dialogRepository.save(dialog)
                            .flatMap(savedDialog ->
                                    participantRepository
                                            .insert(
                                                    savedDialog.getId(),
                                                    userId,
                                                    true,
                                                    now
                                            )
                                            .thenReturn(
                                                    new DialogResponse(
                                                            savedDialog.getId(),
                                                            savedDialog.getDialogName(),
                                                            null,
                                                            null,
                                                            savedDialog.getCreatedAt(),
                                                            savedDialog.isGroupChat()
                                                    )
                                            )
                            );
                });
    }

    @Override
    public Mono<Void> addParticipant(
            Long dialogId,
            Long userId
    ) {
        return userServiceInfo.getCurrentUserId()
                .flatMap(currentUserId ->
                        dialogRepository.findById(dialogId)
                                .switchIfEmpty(
                                        Mono.error(
                                                new ChatHaveNoAccess("Диалог с id %s не найден".formatted(dialogId))
                                        )
                                )
                                .flatMap(dialog -> {

                                    if (!dialog.isGroupChat()) {
                                        return Mono.error(
                                                new IllegalStateException("Пользователь может быть добавлен только в группу")
                                        );
                                    }

                                    return participantRepository
                                            .findByDialogIdAndUserId(
                                                    dialogId,
                                                    currentUserId
                                            )
                                            .switchIfEmpty(
                                                    Mono.error(
                                                            new ChatHaveNoAccess(
                                                                    "Вы не являетесь участником группы"
                                                            )
                                                    )
                                            )
                                            .flatMap(currentParticipant -> {

                                                if (!currentParticipant.isAdmin()) {
                                                    return Mono.error(
                                                            new ChatHaveNoAccess(
                                                                    "Только администратор может добавлять участников"
                                                            )
                                                    );
                                                }

                                                return userService.findById(userId)
                                                        .switchIfEmpty(
                                                                Mono.error(
                                                                        new UserNotFoundException("Пользователь с id %s не найден".formatted(userId))
                                                                )
                                                        )
                                                        .flatMap(user ->
                                                                participantRepository
                                                                        .existsByDialogIdAndUserId(
                                                                                dialogId,
                                                                                userId
                                                                        )
                                                        )
                                                        .flatMap(exists -> {

                                                            if (exists) {
                                                                return Mono.error(
                                                                        new IllegalStateException(
                                                                                "Пользователь уже является участником группы"
                                                                        )
                                                                );
                                                            }

                                                            return participantRepository.insert(
                                                                    dialogId,
                                                                    userId,
                                                                    false,
                                                                    OffsetDateTime.now()
                                                            );
                                                        });
                                            });
                                })
                );
    }

    @Override
    public Flux<DialogListItem> getMyDialogs() {

        return userServiceInfo.getCurrentUser()
                .flatMapMany(user ->
                        dialogRepository
                                .findAllByUserId(user.userId())
                                .flatMap(dialog -> {

                                    Long otherUserId;

                                    if (Objects.equals(
                                            FAVORITE_DIALOG,
                                            dialog.getDialogName()
                                    )) {
                                        otherUserId = user.userId();
                                    } else if (Objects.equals(
                                            dialog.getOwnerId(),
                                            user.userId()
                                    )) {
                                        otherUserId = dialog.getParticipantId();
                                    } else {
                                        otherUserId = dialog.getOwnerId();
                                    }

                                    return Mono.zip(
                                                    userService.findById(otherUserId),
                                                    messageReaderService
                                                            .findLastMessageDateByDialogId(
                                                                    dialog.getId()
                                                            ),
                                                    getUnreadCount(
                                                            dialog.getId(),
                                                            user.userId()
                                                    )
                                            )
                                            .map(tuple -> {

                                                User otherUser = tuple.getT1();

                                                OffsetDateTime lastMessageAt =
                                                        tuple.getT2();

                                                return new DialogListItem(
                                                        dialog.getId(),
                                                        dialog.getDialogName(),
                                                        otherUser,
                                                        lastMessageAt.equals(
                                                                OffsetDateTime.MIN
                                                        )
                                                                ? null
                                                                : lastMessageAt,
                                                        tuple.getT3()
                                                );
                                            });
                                })
                                .sort(
                                        Comparator.comparing(
                                                DialogListItem::lastMessageAt,
                                                Comparator.nullsLast(
                                                        Comparator.reverseOrder()
                                                )
                                        )
                                )
                );
    }

    @Override
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
                                dialog.getCreatedAt(),
                                dialog.isGroupChat()
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
                        checkAccessAndReturnDialog(
                                id,
                                user.userId()
                        )
                )
                .flatMap(dialog -> {

                    /*
                     * Группа или Избранное.
                     * Для них имя уже известно.
                     */
                    if (dialog.isGroupChat()
                            || dialog.getDialogName() != null) {

                        return Mono.just(dialog);
                    }

                    /*
                     * Обычный private dialog.
                     */
                    return userServiceInfo.getCurrentUser()
                            .flatMap(user -> {

                                Long otherUserId;

                                if (Objects.equals(
                                        dialog.getOwnerId(),
                                        user.userId()
                                )) {
                                    otherUserId =
                                            dialog.getParticipantId();
                                } else {
                                    otherUserId =
                                            dialog.getOwnerId();
                                }

                                if (otherUserId == null) {
                                    return Mono.error(
                                            new IllegalStateException(
                                                    "Не удалось определить второго участника диалога "
                                                            + dialog.getId()
                                            )
                                    );
                                }

                                return userService.findById(otherUserId)
                                        .switchIfEmpty(
                                                Mono.error(
                                                        new UserNotFoundException(
                                                                "Пользователь с id %s не найден"
                                                                        .formatted(otherUserId)
                                                        )
                                                )
                                        )
                                        .map(otherUser -> {

                                            String dialogName;

                                            if (otherUser.getName() != null
                                                    && !otherUser.getName().isBlank()) {

                                                dialogName =
                                                        otherUser.getName()
                                                                + " ("
                                                                + otherUser.getEmail()
                                                                + ")";

                                            } else {

                                                dialogName =
                                                        otherUser.getEmail();
                                            }

                                            dialog.setDialogName(dialogName);

                                            return dialog;
                                        });
                            });
                });
    }

    @Override
    public Mono<Void> checkAccess(Long dialogId, Long userId) {
        return participantRepository
                .countByDialogIdAndUserId(dialogId, userId)
                .flatMap(count -> {

                    if (count == 0) {
                        return Mono.error(
                                new ChatHaveNoAccess(
                                        "Нет доступа к диалогу c id [%s]"
                                                .formatted(dialogId)
                                )
                        );
                    }

                    return Mono.empty();
                });
    }

    private Mono<Dialog> checkAccessAndReturnDialog(
            Long dialogId,
            Long userId
    ) {
        return dialogRepository.findById(dialogId)
                .switchIfEmpty(
                        Mono.error(
                                new ChatHaveNoAccess(
                                        "Диалог с id %s не найден"
                                                .formatted(dialogId)
                                )
                        )
                )
                .flatMap(dialog ->
                        participantRepository
                                .existsByDialogIdAndUserId(
                                        dialogId,
                                        userId
                                )
                                .flatMap(isParticipant ->
                                        isParticipant
                                                ? Mono.just(dialog)
                                                : Mono.error(
                                                new ChatHaveNoAccess(
                                                        "Нет доступа к диалогу c id [%s]"
                                                                .formatted(dialogId)
                                                )
                                        )
                                )
                );
    }

    private Mono<Long> getUnreadCount(
            Long dialogId,
            Long userId
    ) {

        return participantRepository
                .findByDialogIdAndUserId(
                        dialogId,
                        userId
                )
                .flatMap(participant -> {

                    Long lastReadMessageId =
                            participant.getLastReadMessageId();

                    if (lastReadMessageId == null) {
                        return messageReaderService
                                .countMessagesAfterId(
                                        dialogId,
                                        null
                                );
                    }

                    return messageReaderService
                            .countMessagesAfterId(
                                    dialogId,
                                    lastReadMessageId
                            );
                })
                .defaultIfEmpty(0L);
    }

    private Mono<DialogResponse> createNewFavoritesDialog(
            Long userId
    ) {

        OffsetDateTime now =
                OffsetDateTime.now();

        Dialog dialog =
                Dialog.builder()
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
                                        true,
                                        now
                                )
                                .thenReturn(
                                        new DialogResponse(
                                                savedDialog.getId(),
                                                FAVORITE_DIALOG,
                                                userId,
                                                FAVORITE_DIALOG,
                                                savedDialog.getCreatedAt(),
                                                dialog.isGroupChat()
                                        )
                                )
                );
    }

    private Mono<DialogResponse> createNewDialog(
            Long currentUserId,
            User otherUser
    ) {

        OffsetDateTime now =
                OffsetDateTime.now();

        Dialog dialog =
                Dialog.builder()
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
                                        false,
                                        now
                                )
                                .then(
                                        participantRepository.insert(
                                                savedDialog.getId(),
                                                otherUser.getId(),
                                                false,
                                                now
                                        )
                                )
                                .thenReturn(
                                        new DialogResponse(
                                                savedDialog.getId(),
                                                savedDialog.getDialogName(),
                                                otherUser.getId(),
                                                otherUser.getEmail(),
                                                savedDialog.getCreatedAt(),
                                                savedDialog.isGroupChat()
                                        )
                                )
                );
    }

    @Override
    public Mono<Void> markAsRead(Long dialogId, Long userId) {

        return participantRepository
                .countByDialogIdAndUserId(dialogId, userId)
                .flatMap(count -> {

                    if (count == 0) {
                        return Mono.error(
                                new ChatHaveNoAccess(
                                        "Нет доступа к диалогу c id [%s]"
                                                .formatted(dialogId)
                                )
                        );
                    }

                    return messageReaderService
                            .findLastMessageId(dialogId)
                            .flatMap(lastMessageId ->
                                    participantRepository
                                            .updateLastReadMessageId(
                                                    dialogId,
                                                    userId,
                                                    lastMessageId
                                            )
                            );
                })
                .doOnError(error ->
                        log.error(
                                "markAsRead ERROR: dialogId={}, userId={}",
                                dialogId,
                                userId,
                                error
                        )
                )
                .then();
    }

    @Override
    public Mono<Long> getLastReadMessageId(Long dialogId, Long userId) {
        return participantRepository
                .findByDialogIdAndUserId(dialogId, userId)
                .flatMap(participant ->
                        Mono.justOrEmpty(participant.getLastReadMessageId())
                );
    }

    @Override
    public Mono<Void> updateGroup(
            Long dialogId,
            String name,
            boolean isPublic
    ) {
        return userServiceInfo.getCurrentUserId()
                .flatMap(currentUserId ->

                        dialogRepository
                                .findGroupById(dialogId)
                                .switchIfEmpty(
                                        Mono.error(
                                                new IllegalStateException(
                                                        "Группа с id %s не найдена"
                                                                .formatted(dialogId)
                                                )
                                        )
                                )
                                .then(
                                        participantRepository
                                                .findByDialogIdAndUserId(
                                                        dialogId,
                                                        currentUserId
                                                )
                                )
                                .switchIfEmpty(
                                        Mono.error(
                                                new ChatHaveNoAccess(
                                                        "Вы не являетесь участником группы"
                                                )
                                        )
                                )
                                .flatMap(participant -> {

                                    if (!participant.isAdmin()) {
                                        return Mono.error(
                                                new ChatHaveNoAccess(
                                                        "Только администратор может изменять настройки группы"
                                                )
                                        );
                                    }

                                    String normalizedName =
                                            name == null
                                                    ? ""
                                                    : name.trim();

                                    if (normalizedName.isBlank()) {
                                        return Mono.error(
                                                new IllegalArgumentException(
                                                        "Название группы не может быть пустым"
                                                )
                                        );
                                    }

                                    return dialogRepository.updateGroup(
                                            dialogId,
                                            normalizedName,
                                            isPublic,
                                            OffsetDateTime.now()
                                    );
                                })
                );
    }

    @Override
    public Mono<Void> removeParticipant(
            Long dialogId,
            Long userId
    ) {
        return userServiceInfo.getCurrentUserId()
                .flatMap(currentUserId ->

                        participantRepository
                                .findByDialogIdAndUserId(
                                        dialogId,
                                        currentUserId
                                )
                                .switchIfEmpty(
                                        Mono.error(
                                                new ChatHaveNoAccess(
                                                        "Вы не являетесь участником группы"
                                                )
                                        )
                                )
                                .flatMap(currentParticipant -> {

                                    if (!currentParticipant.isAdmin()) {
                                        return Mono.error(
                                                new ChatHaveNoAccess(
                                                        "Только администратор может удалять участников"
                                                )
                                        );
                                    }

                                    if (Objects.equals(currentUserId, userId)) {
                                        return Mono.error(
                                                new IllegalArgumentException(
                                                        "Нельзя удалить самого себя"
                                                )
                                        );
                                    }

                                    return participantRepository
                                            .findByDialogIdAndUserId(
                                                    dialogId,
                                                    userId
                                            )
                                            .switchIfEmpty(
                                                    Mono.error(
                                                            new UserNotFoundException(
                                                                    "Пользователь не является участником группы"
                                                            )
                                                    )
                                            )
                                            .then(
                                                    participantRepository.delete(
                                                            dialogId,
                                                            userId
                                                    )
                                            );
                                })
                );
    }

    @Override
    public Mono<Void> makeAdmin(
            Long dialogId,
            Long userId
    ) {
        return userServiceInfo.getCurrentUserId()
                .flatMap(currentUserId ->

                        participantRepository
                                .findByDialogIdAndUserId(
                                        dialogId,
                                        currentUserId
                                )
                                .switchIfEmpty(
                                        Mono.error(
                                                new ChatHaveNoAccess(
                                                        "Вы не являетесь участником группы"
                                                )
                                        )
                                )
                                .flatMap(currentParticipant -> {

                                    if (!currentParticipant.isAdmin()) {
                                        return Mono.error(
                                                new ChatHaveNoAccess(
                                                        "Только администратор может назначать администраторов"
                                                )
                                        );
                                    }

                                    return participantRepository
                                            .findByDialogIdAndUserId(
                                                    dialogId,
                                                    userId
                                            )
                                            .switchIfEmpty(
                                                    Mono.error(
                                                            new UserNotFoundException(
                                                                    "Пользователь не является участником группы"
                                                            )
                                                    )
                                            )
                                            .then(
                                                    participantRepository.makeAdmin(
                                                            dialogId,
                                                            userId
                                                    )
                                            );
                                })
                );
    }

    @Override
    public Mono<Void> removeAdmin(
            Long dialogId,
            Long userId
    ) {
        return userServiceInfo.getCurrentUserId()
                .flatMap(currentUserId ->

                        participantRepository
                                .findByDialogIdAndUserId(
                                        dialogId,
                                        currentUserId
                                )
                                .switchIfEmpty(
                                        Mono.error(
                                                new ChatHaveNoAccess(
                                                        "Вы не являетесь участником группы"
                                                )
                                        )
                                )
                                .flatMap(currentParticipant -> {

                                    if (!currentParticipant.isAdmin()) {
                                        return Mono.error(
                                                new ChatHaveNoAccess(
                                                        "Только администратор может снимать права администратора"
                                                )
                                        );
                                    }

                                    return participantRepository
                                            .findByDialogIdAndUserId(
                                                    dialogId,
                                                    userId
                                            )
                                            .switchIfEmpty(
                                                    Mono.error(
                                                            new UserNotFoundException(
                                                                    "Пользователь не является участником группы"
                                                            )
                                                    )
                                            )
                                            .flatMap(targetParticipant -> {

                                                if (!targetParticipant.isAdmin()) {
                                                    return Mono.empty();
                                                }

                                                return participantRepository
                                                        .countAdmins(dialogId)
                                                        .flatMap(adminCount -> {

                                                            if (adminCount <= 1) {
                                                                return Mono.error(
                                                                        new IllegalStateException(
                                                                                "В группе должен остаться хотя бы один администратор"
                                                                        )
                                                                );
                                                            }

                                                            return participantRepository
                                                                    .removeAdmin(
                                                                            dialogId,
                                                                            userId
                                                                    );
                                                        });
                                            });
                                })
                );
    }

    @Override
    public Mono<Void> leaveGroup(Long dialogId) {

        return userServiceInfo.getCurrentUserId()
                .flatMap(currentUserId ->

                        dialogRepository
                                .findGroupById(dialogId)
                                .switchIfEmpty(
                                        Mono.error(
                                                new IllegalStateException(
                                                        "Группа с id %s не найдена"
                                                                .formatted(dialogId)
                                                )
                                        )
                                )
                                .then(
                                        participantRepository
                                                .findByDialogIdAndUserId(
                                                        dialogId,
                                                        currentUserId
                                                )
                                )
                                .switchIfEmpty(
                                        Mono.error(
                                                new ChatHaveNoAccess(
                                                        "Вы не являетесь участником группы"
                                                )
                                        )
                                )
                                .flatMap(participant -> {

                                    if (!participant.isAdmin()) {
                                        return participantRepository.delete(
                                                dialogId,
                                                currentUserId
                                        );
                                    }

                                    return participantRepository
                                            .countAdmins(dialogId)
                                            .flatMap(adminCount -> {

                                                if (adminCount <= 1) {
                                                    return Mono.error(
                                                            new IllegalStateException(
                                                                    "Нельзя покинуть группу, будучи последним администратором"
                                                            )
                                                    );
                                                }

                                                return participantRepository.delete(
                                                        dialogId,
                                                        currentUserId
                                                );
                                            });
                                })
                );
    }

    @Override
    public Mono<GroupSettingsResponse> getGroupSettings(Long dialogId) {

        return userServiceInfo.getCurrentUserId()
                .flatMap(currentUserId ->

                        dialogRepository
                                .findGroupById(dialogId)
                                .switchIfEmpty(
                                        Mono.error(
                                                new IllegalStateException(
                                                        "Группа с id %s не найдена"
                                                                .formatted(dialogId)
                                                )
                                        )
                                )
                                .flatMap(dialog ->

                                        participantRepository
                                                .findByDialogIdAndUserId(
                                                        dialogId,
                                                        currentUserId
                                                )
                                                .switchIfEmpty(
                                                        Mono.error(
                                                                new ChatHaveNoAccess(
                                                                        "Вы не являетесь участником группы"
                                                                )
                                                        )
                                                )
                                                .flatMap(currentParticipant ->

                                                        participantRepository
                                                                .findByDialogId(dialogId)
                                                                .flatMap(participant ->
                                                                        userService
                                                                                .findById(
                                                                                        participant.getUserId()
                                                                                )
                                                                                .map(user ->
                                                                                        new GroupParticipantResponse(
                                                                                                user.getId(),
                                                                                                user.getName(),
                                                                                                user.getEmail(),
                                                                                                participant.isAdmin()
                                                                                        )
                                                                                )
                                                                )
                                                                .collectList()
                                                                .map(participants ->
                                                                        new GroupSettingsResponse(
                                                                                dialog.getId(),
                                                                                dialog.getDialogName(),
                                                                                dialog.getIsPublic(),
                                                                                currentParticipant.isAdmin(),
                                                                                participants
                                                                        )
                                                                )
                                                )
                                )
                );
    }

}