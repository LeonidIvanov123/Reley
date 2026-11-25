package com.reley.messenger.tg.dto;

import java.time.OffsetDateTime;

public record TgMessageResponse(
        Long id,
        Long chatId,
        Long senderId,
        String senderName,
        String text,
        OffsetDateTime date,
        boolean hasAttachments
) {
}