package com.reley.messenger.message.service;

import com.reley.messenger.attachment.dto.AttachmentResponse;
import com.reley.messenger.dialog.entity.DialogParticipant;
import com.reley.messenger.dialog.repository.DialogParticipantRepository;
import com.reley.messenger.dialog.service.DialogService;
import com.reley.messenger.exception.UserNotFoundException;
import com.reley.messenger.message.dto.MessageEditedDomainEvent;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.ArrayList;
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

                                    return enrichMessages(
                                            dialogId,
                                            userId,
                                            messages
                                    );
                                })
                );
    }

    @Override
    public Flux<MessageResponse> getMessagesAfter(Long dialogId, Long afterId) {
        return userServiceInfo.getCurrentUserId()
                .flatMapMany(userId ->
                        messageRepository
                                .findTopByDialogIdAndIdGreaterThanOrderByIdAsc(
                                        dialogId,
                                        afterId,
                                        15
                                )
                                .collectList()
                                .flatMapMany(messages ->
                                        enrichMessages(
                                                dialogId,
                                                userId,
                                                messages
                                        )
                                )
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

    private Mono<Map<Long, Message>> getReplyMessages(List<Message> messages) {
        List<Long> replyMessageIds = messages.stream()
                .map(Message::getReplyToMessageId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (replyMessageIds.isEmpty()) {
            return Mono.just(Map.of());
        }

        return messageRepository
                .findAllByIds(replyMessageIds)
                .collectMap(Message::getId);
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

                                    Mono<Void> validateReply;

                                    if (request.replyToMessageId() == null) {
                                        validateReply = Mono.empty();
                                    } else {
                                        validateReply = messageRepository
                                                .existsByIdAndDialogId(
                                                        request.replyToMessageId(),
                                                        dialogId
                                                )
                                                .flatMap(exists -> {
                                                    if (exists) {
                                                        return Mono.empty();
                                                    }

                                                    return Mono.error(
                                                            new ResponseStatusException(
                                                                    HttpStatus.BAD_REQUEST,
                                                                    "Сообщение для ответа не найдено"
                                                            )
                                                    );
                                                });
                                    }

                                    return validateReply.then(sendMessageInternal(dialogId, userId, request));
                                })
                );
    }

    private Mono<MessageResponse> sendMessageInternal(
            Long dialogId,
            Long userId,
            SendMessageRequest request
    ) {
        OffsetDateTime now = OffsetDateTime.now();

        Message message = Message.builder()
                .dialogId(dialogId)
                .senderId(userId)
                .content(
                        request.content() == null
                                ? ""
                                : request.content().trim()
                )
                .replyToMessageId(request.replyToMessageId())
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
                                    participantRepository.updateLastReadMessageId(
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
                                            .findById(savedMessage.getSenderId())
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
                                    enrichReply(response, dialogId)
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
                                    ).thenReturn(response)
                            );
                });
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
    public Mono<MessageResponse> editMessage(Long dialogId, Long messageId, String content) {
        String normalizedContent =
                content == null
                        ? ""
                        : content.trim();

        if (normalizedContent.isBlank()) {
            return Mono.error(
                    new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Сообщение не может быть пустым"
                    )
            );
        }

        return userServiceInfo.getCurrentUserId()
                .flatMap(userId ->
                        messageRepository
                                .updateMessageContent(
                                        messageId,
                                        dialogId,
                                        userId,
                                        normalizedContent,
                                        OffsetDateTime.now()
                                )
                                .flatMap(updatedCount -> {

                                    if (updatedCount == 0) {
                                        return Mono.error(
                                                new ResponseStatusException(
                                                        HttpStatus.NOT_FOUND,
                                                        "Сообщение не найдено или недоступно для редактирования"
                                                )
                                        );
                                    }

                                    return messageRepository
                                            .findByIdAndDialogId(messageId, dialogId);
                                })
                                .flatMap(message ->
                                        enrichMessages(
                                                dialogId,
                                                userId,
                                                List.of(message)
                                        )
                                                .next()
                                ) .flatMap(response ->
                                        Mono.fromRunnable(() ->
                                                eventPublisher.publishEvent(
                                                        MessageEditedDomainEvent.builder()
                                                                .messageId(response.id())
                                                                .dialogId(response.dialogId())
                                                                .senderId(response.senderId())
                                                                .content(response.content())
                                                                .editedAt(response.editedAt())
                                                                .build()
                                                )
                                        ).thenReturn(response)
                                )
                );
    }

    @Override
    public Flux<MessageResponse> searchMessages(
            Long dialogId,
            String query,
            int limit,
            int offset
    ) {
        String normalizedQuery =
                query == null
                        ? ""
                        : query.trim();

        if (normalizedQuery.isBlank()) {
            return Flux.empty();
        }

        if (limit <= 0) {
            return Flux.empty();
        }

        int normalizedOffset =
                Math.max(offset, 0);

        return userServiceInfo.getCurrentUserId()
                .flatMapMany(userId ->

                        messageRepository
                                .search(
                                        dialogId,
                                        normalizedQuery,
                                        limit,
                                        normalizedOffset
                                )
                                .collectList()
                                .flatMapMany(messages -> {

                                    if (messages.isEmpty()) {
                                        return Flux.empty();
                                    }

                                    List<Long> messageIds =
                                            messages.stream()
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

                                                        getAttachmentsByMessageId(
                                                                messageIds
                                                        )
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
                                                            attachmentsByMessageId =
                                                            tuple.getT4();

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

                                                                List<AttachmentResponse>
                                                                        attachments =
                                                                        attachmentsByMessageId
                                                                                .getOrDefault(
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

    public Flux<MessageResponse> getMessageContext(
            Long dialogId,
            Long messageId,
            int before,
            int after
    ) {
        return userServiceInfo.getCurrentUserId()
                .flatMapMany(userId ->
                        messageRepository
                                .findByIdAndDialogId(
                                        messageId,
                                        dialogId
                                )
                                .switchIfEmpty(
                                        Mono.error(
                                                new ResponseStatusException(
                                                        HttpStatus.NOT_FOUND,
                                                        "Сообщение не найдено"
                                                )
                                        )
                                )
                                .flatMap(targetMessage -> {

                                    Mono<List<Message>> beforeMessages =
                                            messageRepository
                                                    .findBefore(dialogId, messageId, before)
                                                    .collectList()
                                                    .map(list -> {
                                                        Collections.reverse(list);
                                                        return list;
                                                    });

                                    Mono<List<Message>> afterMessages =
                                            messageRepository
                                                    .findAfter(dialogId, messageId, after)
                                                    .collectList();

                                    return Mono.zip(
                                            beforeMessages,
                                            Mono.just(targetMessage),
                                            afterMessages
                                    );
                                })
                                .flatMapMany(tuple -> {

                                    List<Message> messages =
                                            new ArrayList<>();

                                    messages.addAll(tuple.getT1());
                                    messages.add(tuple.getT2());
                                    messages.addAll(tuple.getT3());

                                    return enrichMessages(
                                            dialogId,
                                            userId,
                                            messages
                                    );
                                })
                );
    }

    private Mono<MessageResponse> enrichReply(MessageResponse response, Long dialogId) {
        if (response.replyToMessageId() == null) {
            return Mono.just(response);
        }

        return messageRepository
                .findByIdAndDialogId(response.replyToMessageId(), dialogId)
                .flatMap(replyMessage ->
                        getSenderInfoMap(dialogId)
                                .map(senderInfoByUserId -> {

                                    MessageResponse.SenderInfo replySender =
                                            senderInfoByUserId.get(
                                                    replyMessage.getSenderId()
                                            );

                                    MessageResponse.ReplyMessageResponse replyTo =
                                            new MessageResponse.ReplyMessageResponse(
                                                    replyMessage.getId(),
                                                    replyMessage.getSenderId(),
                                                    replySender != null
                                                            ? replySender.name()
                                                            : null,
                                                    replyMessage.getDeletedAt() != null
                                                            ? null
                                                            : replyMessage.getContent(),
                                                    replyMessage.getCreatedAt(),
                                                    replyMessage.getDeletedAt() != null
                                            );

                                    return response.withReplyTo(replyTo);
                                })
                )
                .defaultIfEmpty(response);
    }

    private Flux<MessageResponse> enrichMessages(
            Long dialogId,
            Long userId,
            List<Message> messages
    ) {
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
                        otherUserId = dialog.getParticipantId();
                    } else {
                        otherUserId = dialog.getOwnerId();
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

                            getAttachmentsByMessageId(messageIds),

                            getReplyMessages(messages)

                    ).flatMapMany(tuple -> {

                        Long lastReadMessageId = tuple.getT1();

                        Map<Long, List<MessageReactionResponse>>
                                reactionsByMessageId = tuple.getT2();

                        Map<Long, MessageResponse.SenderInfo>
                                senderInfoByUserId = tuple.getT3();

                        Map<Long, List<AttachmentResponse>>
                                attachmentsByMessageId = tuple.getT4();

                        Map<Long, Message>
                                replyMessagesById = tuple.getT5();

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

                                    List<MessageReactionResponse> reactions =
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
                                            attachmentsByMessageId
                                                    .getOrDefault(
                                                            message.getId(),
                                                            List.of()
                                                    );

                                    MessageResponse response =
                                            MessageResponse.fromEntity(
                                                    message,
                                                    readByOther,
                                                    reactions,
                                                    sender,
                                                    attachments
                                            );

                                    if (message.getReplyToMessageId() == null) {
                                        return response;
                                    }

                                    Message replyMessage =
                                            replyMessagesById.get(
                                                    message.getReplyToMessageId()
                                            );

                                    if (replyMessage == null) {
                                        return response;
                                    }

                                    MessageResponse.SenderInfo replySender =
                                            senderInfoByUserId.get(
                                                    replyMessage.getSenderId()
                                            );

                                    String replyContent =
                                            replyMessage.getDeletedAt() != null
                                                    ? null
                                                    : replyMessage.getContent();

                                    MessageResponse.ReplyMessageResponse replyTo =
                                            new MessageResponse.ReplyMessageResponse(
                                                    replyMessage.getId(),
                                                    replyMessage.getSenderId(),
                                                    replySender != null
                                                            ? replySender.name()
                                                            : null,
                                                    replyContent,
                                                    replyMessage.getCreatedAt(),
                                                    replyMessage.getDeletedAt() != null
                                            );

                                    return response.withReplyTo(replyTo);
                                });
                    });
                });
    }

}
