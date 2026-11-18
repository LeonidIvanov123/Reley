package com.reley.messenger.user.dto;

import java.time.OffsetDateTime;

public record UserResponse(
        Long id,
        String email,
        String name,
        String phone,
        String username,
        OffsetDateTime lastSeenAt,
        boolean hiddenFromDirectory,
        String avatar,
        String avatarContentType) {
}
