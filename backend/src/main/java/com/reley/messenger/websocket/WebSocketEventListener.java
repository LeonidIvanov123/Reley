package com.reley.messenger.websocket;

import com.reley.messenger.dialog.entity.DialogParticipant;
import com.reley.messenger.dialog.repository.DialogParticipantRepository;
import com.reley.messenger.dialog.repository.DialogRepository;
import com.reley.messenger.message.dto.MessageEditedDomainEvent;
import com.reley.messenger.message.dto.MessageReadDomainEvent;
import com.reley.messenger.message.dto.NewMessageDomainEvent;
import com.reley.messenger.message.reaction.dto.MessageReactionChangedDomainEvent;
import com.reley.messenger.message.service.MessageReaderService;
import com.reley.messenger.websocket.event.DialogUpdatedEvent;
import com.reley.messenger.websocket.event.MessageEditedEvent;
import com.reley.messenger.websocket.event.MessageReactionChangedWebSocketEvent;
import com.reley.messenger.websocket.event.MessageReadEvent;
import com.reley.messenger.websocket.event.NewMessageEvent;
import com.reley.messenger.websocket.event.WebSocketEvent;
import com.reley.messenger.websocket.event.WebSocketEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final DialogRepository dialogRepository;
    private final DialogParticipantRepository dialogParticipantRepository;
    private final MessageReaderService messageReaderService;
    private final ReleyWebSocketService webSocketService;

    @Async
    @EventListener
    public void handle(NewMessageDomainEvent event) {

        WebSocketEvent newMessageEvent =
                new WebSocketEvent(
                        WebSocketEventType.NEW_MESSAGE,
                        event.dialogId(),
                        new NewMessageEvent(
                                event.id(),
                                event.dialogId(),
                                event.senderId(),
                                event.senderName(),
                                event.senderEmail(),
                                event.content(),
                                event.createdAt(),
                                event.attachments(),
                                event.replyTo()
                        )
                );

        dialogRepository
                .findById(event.dialogId())
                .flatMapMany(dialog -> {

                    String messagePreview =
                            createMessagePreview(
                                    event,
                                    dialog.isGroupChat()
                            );

                    return dialogParticipantRepository
                            .findByDialogId(event.dialogId())
                            .flatMap(participant -> {

                                Long userId =
                                        participant.getUserId();

                                Mono<Void> dialogUpdated =
                                        sendDialogUpdated(
                                                event.dialogId(),
                                                userId,
                                                dialog.getDialogName(),
                                                dialog.isGroupChat(),
                                                messagePreview,
                                                event.createdAt()
                                        );

                                if (Objects.equals(
                                        userId,
                                        event.senderId()
                                )) {
                                    return dialogUpdated;
                                }

                                log.debug(
                                        "Sending NEW_MESSAGE: " +
                                                "dialogId={}, messageId={}, " +
                                                "senderId={}, recipientId={}",
                                        event.dialogId(),
                                        event.id(),
                                        event.senderId(),
                                        userId
                                );

                                return webSocketService
                                        .sendToUser(
                                                userId,
                                                newMessageEvent
                                        )
                                        .doOnSuccess(v ->
                                                log.debug(
                                                        "NEW_MESSAGE sent: " +
                                                                "recipientId={}",
                                                        userId
                                                )
                                        )
                                        .then(dialogUpdated);
                            });
                })
                .doOnError(error ->
                        log.error(
                                "Failed to handle NewMessageDomainEvent: " +
                                        "dialogId={}, messageId={}, senderId={}",
                                event.dialogId(),
                                event.id(),
                                event.senderId(),
                                error
                        )
                )
                .subscribe();
    }

    @Async
    @EventListener
    public void handleMessageEdited(MessageEditedDomainEvent event) {

        WebSocketEvent webSocketEvent =
                new WebSocketEvent(
                        WebSocketEventType.MESSAGE_EDITED,
                        event.dialogId(),
                        new MessageEditedEvent(
                                event.messageId(),
                                event.senderId(),
                                event.content(),
                                event.editedAt()
                        )
                );

        dialogParticipantRepository
                .findByDialogId(event.dialogId())
                .map(DialogParticipant::getUserId)
                .flatMap(userId ->
                        webSocketService.sendToUser(userId, webSocketEvent)
                )
                .doOnError(error ->
                        log.error(
                                "Failed to send MESSAGE_EDITED event: "
                                        + "dialogId={}, messageId={}, senderId={}",
                                event.dialogId(),
                                event.messageId(),
                                event.senderId(),
                                error
                        )
                )
                .subscribe();
    }

    @Async
    @EventListener
    public void handleMessageRead(MessageReadDomainEvent event) {

        dialogRepository.findById(event.dialogId())
                .flatMap(dialog -> {

                    Long recipientUserId;

                    if (Objects.equals(dialog.getOwnerId(), event.userId())) {

                        recipientUserId = dialog.getParticipantId();
                    } else {
                        recipientUserId = dialog.getOwnerId();
                    }

                    WebSocketEvent webSocketEvent =
                            new WebSocketEvent(
                                    WebSocketEventType.MESSAGE_READ,
                                    event.dialogId(),
                                    new MessageReadEvent(event.lastReadMessageId())
                            );

                    return webSocketService.sendToUser(
                            recipientUserId,
                            webSocketEvent
                    );
                })
                .doOnError(error ->
                        log.error(
                                "Failed to send MESSAGE_READ event: dialogId={}, userId={}",
                                event.dialogId(),
                                event.userId(),
                                error
                        )
                )
                .subscribe();
    }

    private Mono<Void> sendDialogUpdated(
            Long dialogId,
            Long userId,
            String dialogName,
            boolean groupChat,
            String lastMessagePreview,
            OffsetDateTime lastMessageAt
    ) {
        return dialogParticipantRepository
                .findByDialogIdAndUserId(dialogId, userId)
                .flatMap(participant -> {

                    Long lastReadMessageId =
                            participant.getLastReadMessageId();

                    return messageReaderService.countMessagesAfterId(
                            dialogId,
                            lastReadMessageId
                    );
                })
                .switchIfEmpty(
                        messageReaderService.countMessagesAfterId(
                                dialogId,
                                null
                        )
                )
                .map(unreadCount ->
                        new WebSocketEvent(
                                WebSocketEventType.DIALOG_UPDATED,
                                dialogId,
                                new DialogUpdatedEvent(
                                        dialogId,
                                        dialogName,
                                        groupChat,
                                        lastMessagePreview,
                                        unreadCount,
                                        lastMessageAt
                                )
                        )
                )
                .flatMap(event ->
                        webSocketService.sendToUser(
                                userId,
                                event
                        )
                );
    }

    @Async
    @EventListener
    public void handleMessageReactionChanged(
            MessageReactionChangedDomainEvent event
    ) {

        WebSocketEvent webSocketEvent =
                new WebSocketEvent(
                        WebSocketEventType.MESSAGE_REACTION_CHANGED,
                        event.dialogId(),
                        new MessageReactionChangedWebSocketEvent(
                                event.messageId(),
                                event.userId(),
                                event.oldReaction(),
                                event.newReaction(),
                                event.reactions()
                        )
                );

        dialogParticipantRepository
                .findByDialogId(event.dialogId())
                .map(DialogParticipant::getUserId)
                .filter(userId ->
                        !Objects.equals(userId, event.userId())
                )
                .flatMap(userId ->
                        webSocketService.sendToUser(
                                userId,
                                webSocketEvent
                        )
                )
                .doOnError(error ->
                        log.error(
                                "Failed to send MESSAGE_REACTION_CHANGED event: "
                                        + "dialogId={}, messageId={}, userId={}",
                                event.dialogId(),
                                event.messageId(),
                                event.userId(),
                                error
                        )
                )
                .subscribe();
    }

    private String createMessagePreview(
            NewMessageDomainEvent event,
            boolean groupChat
    ) {
        String preview;

        if (event.content() == null
                || event.content().isBlank()) {

            if (event.attachments() != null
                    && !event.attachments().isEmpty()) {

                preview = "📎 Вложение";

            } else {

                preview = "";
            }

        } else {

            preview = event.content()
                    .replaceAll("\\s+", " ")
                    .trim();
        }

        if (groupChat) {

            String senderName =
                    event.senderName() != null
                            && !event.senderName().isBlank()
                            ? event.senderName()
                            : event.senderEmail();

            if (senderName != null
                    && !senderName.isBlank()) {

                preview =
                        senderName + ": " + preview;
            }
        }

        if (preview.length() > 50) {
            preview =
                    preview.substring(0, 50) + "…";
        }

        return preview;
    }

}
