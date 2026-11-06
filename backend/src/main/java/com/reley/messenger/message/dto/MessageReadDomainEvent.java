package com.reley.messenger.message.dto;

public record MessageReadDomainEvent(
        Long dialogId,
        Long userId,
        Long lastReadMessageId
) {
}
