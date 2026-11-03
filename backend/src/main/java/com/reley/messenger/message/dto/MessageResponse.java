package com.reley.messenger.message.dto;

import com.reley.messenger.message.entity.Message;

import java.time.OffsetDateTime;

public record MessageResponse(
        Long id,
        Long dialogId,
        Long senderId,
        String senderName,
        String content,
        OffsetDateTime createdAt,
        OffsetDateTime editedAt,
        OffsetDateTime deletedAt
) {

    public static MessageResponse fromEntity(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getDialogId(),
                message.getSenderId(),
                null,
                message.getContent(),
                message.getCreatedAt(),
                message.getEditedAt(),
                message.getDeletedAt()
        );
    }

}