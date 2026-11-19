package com.reley.messenger.message.dto;

import com.reley.messenger.attachment.dto.AttachmentResponse;
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
        List<MessageReactionResponse> reactions,
        List<AttachmentResponse> attachments,
        Long replyToMessageId,
        ReplyMessageResponse replyTo
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
                List.of(),
                List.of(),
                message.getReplyToMessageId(),
                null
        );
    }

    public static MessageResponse fromEntity(
            Message message,
            boolean readByOther,
            List<MessageReactionResponse> reactions,
            SenderInfo sender,
            List<AttachmentResponse> attachments
    ) {
        return new MessageResponse(
                message.getId(),
                message.getDialogId(),
                message.getSenderId(),
                sender.name(),
                sender.email(),
                message.getContent(),
                message.getCreatedAt(),
                message.getEditedAt(),
                message.getDeletedAt(),
                readByOther,
                reactions,
                attachments,
                message.getReplyToMessageId(),
                null
        );
    }

    public MessageResponse withReplyTo(ReplyMessageResponse replyTo) {
        return new MessageResponse(
                id,
                dialogId,
                senderId,
                senderName,
                senderEmail,
                content,
                createdAt,
                editedAt,
                deletedAt,
                readByOther,
                reactions,
                attachments,
                replyToMessageId,
                replyTo
        );
    }

    public record SenderInfo(
            String name,
            String email
    ) {
    }

    public record ReplyMessageResponse(
            Long id,
            Long senderId,
            String senderName,
            String content,
            OffsetDateTime createdAt,
            boolean deleted
    ) {
    }
}