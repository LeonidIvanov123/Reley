package com.reley.messenger.message.dto;

import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record MessageEditedDomainEvent(
        Long messageId,
        Long dialogId,
        Long senderId,
        String content,
        OffsetDateTime editedAt
) {
}