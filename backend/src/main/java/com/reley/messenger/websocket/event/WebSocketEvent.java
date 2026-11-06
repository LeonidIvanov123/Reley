package com.reley.messenger.websocket.event;

public record WebSocketEvent(
        WebSocketEventType type,
        Long dialogId,
        Object payload
) {
}