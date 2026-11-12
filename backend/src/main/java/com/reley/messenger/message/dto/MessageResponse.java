package com.reley.messenger.message.dto;

import com.reley.messenger.message.entity.Message;
import com.reley.messenger.message.reaction.dto.MessageReactionResponse;

import java.time.OffsetDateTime;
import java.util.List;

public record MessageResponse(
        Long id,
        Long dialogId,
        Long senderId,
        String senderName,
        String senderEmail,
        String content,
        OffsetDateTime createdAt,
        OffsetDateTime editedAt,
        OffsetDateTime deletedAt,
        boolean readByOther,
        List<MessageReactionResponse> reactions
) {

    public static MessageResponse fromEntity(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getDialogId(),
                message.getSenderId(),
                null,
                null,
                message.getContent(),
                message.getCreatedAt(),
                message.getEditedAt(),
                message.getDeletedAt(),
                false,
                List.of()
        );
    }

    public static MessageResponse fromEntity(Message message, boolean readByOther) {
        return new MessageResponse(
                message.getId(),
                message.getDialogId(),
                message.getSenderId(),
                null,
                null,
                message.getContent(),
                message.getCreatedAt(),
                message.getEditedAt(),
                message.getDeletedAt(),
                readByOther,
                List.of()
        );
    }

    public static MessageResponse fromEntity(Message message,
                                             boolean readByOther,
                                             List<MessageReactionResponse> reactions,
                                             SenderInfo sender) {
        return new MessageResponse(
                message.getId(),
                message.getDialogId(),
                message.getSenderId(),
                sender.name,
                sender.email,
                message.getContent(),
                message.getCreatedAt(),
                message.getEditedAt(),
                message.getDeletedAt(),
                readByOther,
                reactions
        );
    }

    public record SenderInfo(String name, String email) {
    }

}