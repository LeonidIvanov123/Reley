package com.reley.messenger.tg.dto;

public record TgChatInfo(
        long chatId,
        String title,
        TgChatSendStatus sendStatus,
        boolean canSend
) {
}