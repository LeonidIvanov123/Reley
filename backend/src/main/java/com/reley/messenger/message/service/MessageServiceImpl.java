package com.reley.messenger.message.service;

import com.reley.messenger.dialog.entity.Dialog;
import com.reley.messenger.dialog.service.DialogService;
import com.reley.messenger.exception.ChatHaveNoAccess;
import com.reley.messenger.message.dto.MessageResponse;
import com.reley.messenger.message.dto.SendMessageRequest;
import com.reley.messenger.message.entity.Message;
import com.reley.messenger.message.repository.MessageRepository;
import com.reley.messenger.user.service.HydrateUserInfoApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final DialogService dialogService;
    private final MessageRepository messageRepository;
    private final HydrateUserInfoApi userServiceInfo;

    @Override
    public Flux<MessageResponse> getMessages(Long dialogId, Long beforeId) {
        return userServiceInfo.getCurrentUserId()
                .flatMapMany(userId ->
                        checkAccess(dialogId, userId)
                                .thenMany(
                                        beforeId == null
                                                ? messageRepository
                                                .findTop15ByDialogIdOrderByIdDesc(dialogId)
                                                : messageRepository
                                                .findTop15ByDialogIdAndIdLessThanOrderByIdDesc(
                                                        dialogId,
                                                        beforeId
                                                )
                                )
                                .collectList()
                                .flatMapMany(messages -> {
                                    Collections.reverse(messages);

                                    return Flux.fromIterable(messages);
                                })
                )
                .map(MessageResponse::fromEntity);
    }

    private Mono<Void> checkAccess(Long dialogId, Long userId) {

        return dialogService.findById(dialogId)
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
                                ? Mono.empty()
                                : Mono.error(
                                        new ChatHaveNoAccess("Нет доступа к диалогу c id [%s]".formatted(dialogId)
                        ));
                });
    }


    @Override
    public Mono<MessageResponse> sendMessage(Long dialogId, SendMessageRequest request) {

            return userServiceInfo.getCurrentUserId()
                    .flatMap(userId ->
                            checkAccess(dialogId, userId)
                                    .then(Mono.defer(() -> {

                                        OffsetDateTime now = OffsetDateTime.now();

                                        Message message = Message.builder()
                                                .dialogId(dialogId)
                                                .senderId(userId)
                                                .content(request.content().trim())
                                                .createdAt(now)
                                                .build();

                                        return messageRepository.save(message);
                                    }))
                                    .map(MessageResponse::fromEntity)
                    );
        }

    @Override
    public Mono<MessageResponse> deleteMessage(Long dialogId, Long messageId) {
        return userServiceInfo.getCurrentUser()
                .flatMap(user ->
                        messageRepository.markAsDeleteMessage(messageId, dialogId, user.userId(), OffsetDateTime.now()))
                .flatMap(count -> messageRepository.findById(messageId))
                .map(MessageResponse::fromEntity);
    }

    @Override
    public Mono<OffsetDateTime> findLastMessageDateByDialogId(Long dialogId) {
        return messageRepository.findLastMessageDate(dialogId);
    }

}
