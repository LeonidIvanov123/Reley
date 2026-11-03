package com.reley.messenger.dialog.dto;

import java.time.OffsetDateTime;

public record DialogResponse(
        Long id,
        String dialogName,
        Long otherUserId,
        String otherUserEmail,
        OffsetDateTime createdAt
) {}
