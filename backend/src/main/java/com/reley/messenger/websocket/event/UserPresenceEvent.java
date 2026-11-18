package com.reley.messenger.websocket.event;

import java.time.OffsetDateTime;

public record UserPresenceEvent(
        Long userId,
        OffsetDateTime lastSeenAt
) {
}