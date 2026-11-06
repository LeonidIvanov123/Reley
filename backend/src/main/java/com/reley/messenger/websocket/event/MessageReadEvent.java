package com.reley.messenger.websocket.event;

public record MessageReadEvent(
        long lastReadMessageId
) {
}
