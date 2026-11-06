package com.reley.messenger.websocket.event;

import java.time.OffsetDateTime;

public record DialogUpdatedEvent(
        Long dialogId,
        long unreadCount,
        OffsetDateTime lastMessageAt
) {
}