package com.reley.messenger.tg.dto;

import java.time.OffsetDateTime;

public record TgDialogResponse(
        long chatId,
        String title,
        String type,
        String lastMessage,
        OffsetDateTime lastMessageAt
) {
}