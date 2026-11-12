package com.reley.messenger.websocket;

import com.reley.messenger.dialog.entity.DialogParticipant;
import com.reley.messenger.dialog.repository.DialogParticipantRepository;
import com.reley.messenger.dialog.repository.DialogRepository;
import com.reley.messenger.message.dto.MessageReadDomainEvent;
import com.reley.messenger.message.dto.NewMessageDomainEvent;
import com.reley.messenger.message.reaction.dto.MessageReactionChangedDomainEvent;
import com.reley.messenger.message.service.MessageReaderService;
import com.reley.messenger.websocket.event.DialogUpdatedEvent;
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

        WebSocketEvent webSocketEvent =
                new WebSocketEvent(
                        WebSocketEventType.NEW_MESSAGE,
                        event.dialogId(),
                        new NewMessageEvent(
                                event.id(),
                                event.dialogId(),
                                event.senderId(),
                                event.content(),
                                event.createdAt()
                        )
                );

        dialogParticipantRepository
                .findByDialogId(event.dialogId())
                .map(DialogParticipant::getUserId)
                .filter(userId -> !Objects.equals(userId, event.senderId()))
                .flatMap(userId ->
                        webSocketService
                                .sendToUser(userId, webSocketEvent)
                                .then(
                                        sendDialogUpdated(
                                                event.dialogId(),
                                                userId,
                                                event.createdAt()
                                        )
                                )
                )
                .doOnError(error ->
                        log.error(
                                "Failed to send NEW_MESSAGE event: "
                                        + "dialogId={}, messageId={}, senderId={}",
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
            OffsetDateTime lastMessageAt
    ) {
        return dialogParticipantRepository
                .findByDialogIdAndUserId(dialogId, userId)
                .map(DialogParticipant::getLastReadMessageId)
                .flatMap(Mono::justOrEmpty)
                .flatMap(lastReadMessageId ->
                        messageReaderService.countMessagesAfterId(
                                dialogId,
                                lastReadMessageId
                        )
                )
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

}
