package com.reley.messenger.search.dto;

import java.time.OffsetDateTime;

public record SearchMessageResponse(
        Long messageId,
        Long dialogId,
        String dialogName,
        Long senderId,
        String senderName,
        String senderEmail,
        String content,
        OffsetDateTime createdAt
) {
}