package com.reley.messenger.message.service;

import com.reley.messenger.dialog.repository.DialogParticipantRepository;
import com.reley.messenger.dialog.service.DialogService;
import com.reley.messenger.exception.ChatHaveNoAccess;
import com.reley.messenger.message.dto.MessageResponse;
import com.reley.messenger.message.dto.NewMessageDomainEvent;
import com.reley.messenger.message.dto.SendMessageRequest;
import com.reley.messenger.message.entity.Message;
import com.reley.messenger.message.reaction.dto.MessageReactionResponse;
import com.reley.messenger.message.reaction.service.MessageReactionService;
import com.reley.messenger.message.repository.MessageRepository;
import com.reley.messenger.user.service.HydrateUserInfoApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final DialogService dialogService;
    private final MessageRepository messageRepository;
    private final HydrateUserInfoApi userServiceInfo;
    private final ApplicationEventPublisher eventPublisher;
    private final MessageReactionService messageReactionService;
    private final DialogParticipantRepository participantRepository;

    @Override
    public Mono<Message> findTopByDialogIdOrderByIdDesc(Long dialogId) {
        return messageRepository.findTopByDialogIdOrderByIdDesc(dialogId);
    }

    @Override
    public Flux<MessageResponse> getMessages(
            Long dialogId,
            Long beforeId
    ) {
        return userServiceInfo.getCurrentUserId()
                .flatMapMany(userId ->

                        (
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

                                    if (messages.isEmpty()) {
                                        return Flux.empty();
                                    }

                                    List<Long> messageIds = messages.stream()
                                            .map(Message::getId)
                                            .toList();

                                    return dialogService
                                            .findById(dialogId)
                                            .flatMapMany(dialog -> {

                                                Long otherUserId;

                                                if (Objects.equals(
                                                        dialog.getOwnerId(),
                                                        userId
                                                )) {
                                                    otherUserId =
                                                            dialog.getParticipantId();
                                                } else {
                                                    otherUserId =
                                                            dialog.getOwnerId();
                                                }

                                                return Mono.zip(
                                                        dialogService
                                                                .getLastReadMessageId(
                                                                        dialogId,
                                                                        otherUserId
                                                                )
                                                                .defaultIfEmpty(0L),

                                                        messageReactionService
                                                                .getReactions(
                                                                        messageIds,
                                                                        userId
                                                                )
                                                ).flatMapMany(tuple -> {

                                                    Long lastReadMessageId =
                                                            tuple.getT1();

                                                    Map<Long, List<MessageReactionResponse>>
                                                            reactionsByMessageId =
                                                            tuple.getT2();

                                                    return Flux
                                                            .fromIterable(messages)
                                                            .map(message -> {

                                                                boolean readByOther =
                                                                        Objects.equals(
                                                                                message.getSenderId(),
                                                                                userId
                                                                        )
                                                                                && message.getId()
                                                                                <= lastReadMessageId;

                                                                List<MessageReactionResponse>
                                                                        reactions =
                                                                        reactionsByMessageId
                                                                                .getOrDefault(
                                                                                        message.getId(),
                                                                                        List.of()
                                                                                );

                                                                return MessageResponse.fromEntity(
                                                                        message,
                                                                        readByOther,
                                                                        reactions
                                                                );
                                                            });
                                                });
                                            });
                                })
                );
    }

    @Override
    public Mono<MessageResponse> sendMessage(
            Long dialogId,
            SendMessageRequest request
    ) {
        return userServiceInfo.getCurrentUserId()
                .flatMap(userId ->
                        dialogService
                                .checkAccess(dialogId, userId)
                                .then(dialogService.findById(dialogId))
                                .flatMap(dialog -> {

                                    OffsetDateTime now = OffsetDateTime.now();

                                    Message message = Message.builder()
                                            .dialogId(dialogId)
                                            .senderId(userId)
                                            .content(request.content().trim())
                                            .createdAt(now)
                                            .build();

                                    return messageRepository
                                            .save(message)
                                            .flatMap(savedMessage -> {

                                                MessageResponse response =
                                                        MessageResponse.fromEntity(
                                                                savedMessage,
                                                                false
                                                        );

                                                return participantRepository
                                                        .updateLastReadMessageId(
                                                                dialogId,
                                                                userId,
                                                                savedMessage.getId()
                                                        )
                                                        .then(Mono.fromRunnable(() ->
                                                                eventPublisher.publishEvent(
                                                                        NewMessageDomainEvent
                                                                                .fromMessageResponse(response)
                                                                )
                                                        ))
                                                        .thenReturn(response);
                                            });
                                })
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
