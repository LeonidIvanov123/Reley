package com.reley.messenger.message.service;

import com.reley.messenger.attachment.dto.AttachmentResponse;
import com.reley.messenger.dialog.repository.DialogParticipantRepository;
import com.reley.messenger.dialog.service.DialogService;
import com.reley.messenger.exception.UserNotFoundException;
import com.reley.messenger.message.dto.MessageResponse;
import com.reley.messenger.message.dto.NewMessageDomainEvent;
import com.reley.messenger.message.dto.SendMessageRequest;
import com.reley.messenger.message.entity.Message;
import com.reley.messenger.message.reaction.dto.MessageReactionResponse;
import com.reley.messenger.message.reaction.service.MessageReactionService;
import com.reley.messenger.message.repository.MessageAttachmentRepository;
import com.reley.messenger.message.repository.MessageRepository;
import com.reley.messenger.user.dto.UserSenderProjection;
import com.reley.messenger.user.service.HydrateUserInfoApi;
import com.reley.messenger.user.service.UserService;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final DialogService dialogService;
    private final MessageRepository messageRepository;
    private final HydrateUserInfoApi userServiceInfo;
    private final ApplicationEventPublisher eventPublisher;
    private final MessageReactionService messageReactionService;
    private final DialogParticipantRepository participantRepository;
    private final UserService userService;
    private final MessageAttachmentService messageAttachmentService;

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
                                                                ),

                                                        getSenderInfoMap(dialogId),

                                                        getAttachmentsByMessageId(messageIds)
                                                ).flatMapMany(tuple -> {

                                                    Long lastReadMessageId =
                                                            tuple.getT1();

                                                    Map<Long, List<MessageReactionResponse>>
                                                            reactionsByMessageId =
                                                            tuple.getT2();

                                                    Map<Long, MessageResponse.SenderInfo>
                                                            senderInfoByUserId =
                                                            tuple.getT3();

                                                    Map<Long, List<AttachmentResponse>>
                                                            attachmentsByMessageId = tuple.getT4();

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

                                                                MessageResponse.SenderInfo sender =
                                                                        senderInfoByUserId.get(
                                                                                message.getSenderId()
                                                                        );

                                                                if (sender == null) {
                                                                    sender =
                                                                            new MessageResponse.SenderInfo(
                                                                                    null,
                                                                                    null
                                                                            );
                                                                }

                                                                List<AttachmentResponse> attachments =
                                                                        attachmentsByMessageId.getOrDefault(
                                                                                message.getId(),
                                                                                List.of()
                                                                        );

                                                                return MessageResponse.fromEntity(
                                                                        message,
                                                                        readByOther,
                                                                        reactions,
                                                                        sender,
                                                                        attachments
                                                                );
                                                            });
                                                });
                                            });
                                })
                );
    }

    private Mono<Map<Long, List<AttachmentResponse>>> getAttachmentsByMessageId(List<Long> messageIds) {
        return messageAttachmentService
                .findAttachmentsByMessageIds(messageIds)
                .map(item ->
                        Map.entry(
                                item.messageId(),
                                AttachmentResponse.fromEntity(
                                        item.attachment()
                                )
                        )
                )
                .collectList()
                .map(entries ->
                        entries.stream()
                                .collect(Collectors.groupingBy(
                                        Map.Entry::getKey,
                                        Collectors.mapping(
                                                Map.Entry::getValue,
                                                Collectors.toList()
                                        )
                                ))
                );
    }

    private Mono<Map<Long, MessageResponse.SenderInfo>> getSenderInfoMap(
            Long dialogId
    ) {
        return userService
                .findSendersByDialogId(dialogId)
                .collectMap(
                        UserSenderProjection::getId,
                        sender -> new MessageResponse.SenderInfo(
                                sender.getName(),
                                sender.getEmail()
                        )
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

                                    OffsetDateTime now =
                                            OffsetDateTime.now();

                                    Message message = Message.builder()
                                            .dialogId(dialogId)
                                            .senderId(userId)
                                            .content(
                                                    request.content() == null
                                                            ? ""
                                                            : request.content().trim()
                                            )
                                            .createdAt(now)
                                            .build();

                                    return messageRepository
                                            .save(message)
                                            .flatMap(savedMessage -> {

                                                Mono<Void> attachAttachments =
                                                        Flux.fromIterable(request.attachmentIds())
                                                                .concatMap(attachmentId ->
                                                                        messageAttachmentService.attachToMessage(
                                                                                attachmentId,
                                                                                dialogId,
                                                                                savedMessage.getId()
                                                                        )
                                                                )
                                                                .then();

                                                return attachAttachments
                                                        .then(
                                                                participantRepository
                                                                        .updateLastReadMessageId(
                                                                                dialogId,
                                                                                userId,
                                                                                savedMessage.getId()
                                                                        )
                                                        )
                                                        .then(
                                                                messageAttachmentService
                                                                        .findAttachmentsByMessageId(
                                                                                savedMessage.getId()
                                                                        )
                                                                        .map(AttachmentResponse::fromEntity)
                                                                        .collectList()
                                                        )
                                                        .flatMap(attachments ->
                                                                userService
                                                                        .findById(
                                                                                savedMessage.getSenderId()
                                                                        )
                                                                        .switchIfEmpty(
                                                                                Mono.error(
                                                                                        new UserNotFoundException(
                                                                                                "Пользователь с id %s не найден"
                                                                                                        .formatted(
                                                                                                                savedMessage.getSenderId()
                                                                                                        )
                                                                                        )
                                                                                )
                                                                        )
                                                                        .map(user ->
                                                                                new MessageResponse.SenderInfo(
                                                                                        user.getName(),
                                                                                        user.getEmail()
                                                                                )
                                                                        )
                                                                        .map(sender ->
                                                                                MessageResponse.fromEntity(
                                                                                        savedMessage,
                                                                                        false,
                                                                                        List.of(),
                                                                                        sender,
                                                                                        attachments
                                                                                )
                                                                        )
                                                        )
                                                        .flatMap(response ->
                                                                Mono.fromRunnable(() ->
                                                                                eventPublisher.publishEvent(
                                                                                        NewMessageDomainEvent.fromMessageResponse(
                                                                                                response,
                                                                                                new MessageResponse.SenderInfo(
                                                                                                        response.senderName(),
                                                                                                        response.senderEmail()
                                                                                                ),
                                                                                                response.attachments()
                                                                                        )
                                                                                )
                                                                        )
                                                                        .thenReturn(response)
                                                        );
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

}
