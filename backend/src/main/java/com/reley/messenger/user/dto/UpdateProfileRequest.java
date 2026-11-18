package com.reley.messenger.user.dto;

import java.time.OffsetDateTime;

public record UpdateProfileRequest(
        String email, String name, String phone,
        Boolean hiddenFromDirectory, String username, OffsetDateTime lastSeenAt) {
}
