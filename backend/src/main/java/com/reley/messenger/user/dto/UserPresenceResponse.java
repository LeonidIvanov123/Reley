package com.reley.messenger.user.dto;

import java.time.OffsetDateTime;

public record UserPresenceResponse(
        Long userId,
        boolean online,
        OffsetDateTime lastSeenAt
) {
}
