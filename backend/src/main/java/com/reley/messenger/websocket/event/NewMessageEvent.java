package com.reley.messenger.websocket.event;

import java.time.OffsetDateTime;

public record NewMessageEvent(
        Long id,
        Long dialogId,
        Long senderId,
        String content,
        OffsetDateTime createdAt
) {
}
