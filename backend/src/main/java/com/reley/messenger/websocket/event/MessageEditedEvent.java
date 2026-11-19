package com.reley.messenger.websocket.event;

import java.time.OffsetDateTime;

public record MessageEditedEvent(
        Long messageId,
        Long senderId,
        String content,
        OffsetDateTime editedAt
) {
}