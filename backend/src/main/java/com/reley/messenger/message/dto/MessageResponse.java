package com.reley.messenger.message.dto;

import com.reley.messenger.attachment.dto.AttachmentResponse;
import com.reley.messenger.message.entity.Message;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record MessageResponse(
        Long id,
        Long dialogId,
        Long senderId,
        String senderName,
        String content,
        OffsetDateTime createdAt,
        OffsetDateTime editedAt,
        OffsetDateTime deletedAt,
        boolean readByOther,
        Map<UUID, AttachmentResponse> attachments
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
                message.getDeletedAt(),
                false,
                Map.of()
        );
    }

    public static MessageResponse fromEntity(Message message, boolean readByOther) {
        return new MessageResponse(
                message.getId(),
                message.getDialogId(),
                message.getSenderId(),
                null,
                message.getContent(),
                message.getCreatedAt(),
                message.getEditedAt(),
                message.getDeletedAt(),
                readByOther,
                Map.of()
        );
    }

}