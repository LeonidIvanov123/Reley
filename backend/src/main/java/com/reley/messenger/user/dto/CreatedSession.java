package com.reley.messenger.user.dto;

import java.util.UUID;

public record CreatedSession(
        UUID sessionId,
        String refreshToken
) {}
