package com.reley.messenger.message.dto;

import com.reley.messenger.attachment.dto.AttachmentResponse;

import java.time.OffsetDateTime;
import java.util.List;


//WebSocket event
public record NewMessageDomainEvent(
        Long id,
        Long dialogId,
        Long senderId,
        String senderName,
        String senderEmail,
        String content,
        OffsetDateTime createdAt,
        List<AttachmentResponse> attachments
) {

    public static NewMessageDomainEvent fromMessageResponse(MessageResponse message,
                                                            MessageResponse.SenderInfo senderInfo,
                                                            List<AttachmentResponse> attachments) {
        return new NewMessageDomainEvent(
                message.id(),
                message.dialogId(),
                message.senderId(),
                senderInfo.name(),
                senderInfo.email(),
                message.content(),
                message.createdAt(),
                attachments
        );
    }

}
