package com.reley.messenger.user.dto;

import java.util.UUID;

public record RotatedRefreshToken(
        Long userId,
        UUID sessionId,
        String refreshToken
) {}