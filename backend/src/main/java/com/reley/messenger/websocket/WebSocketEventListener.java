package com.reley.messenger.websocket;

import com.reley.messenger.dialog.repository.DialogRepository;
import com.reley.messenger.message.dto.MessageReadDomainEvent;
import com.reley.messenger.message.dto.NewMessageDomainEvent;
import com.reley.messenger.websocket.event.MessageReadEvent;
import com.reley.messenger.websocket.event.NewMessageEvent;
import com.reley.messenger.websocket.event.WebSocketEvent;
import com.reley.messenger.websocket.event.WebSocketEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final DialogRepository dialogRepository;
    private final ReleyWebSocketService webSocketService;

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

        webSocketService
                .sendToUser(event.otherUserId(), webSocketEvent)
                .subscribe();
    }

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
}
