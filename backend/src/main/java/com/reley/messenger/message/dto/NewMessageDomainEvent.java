package com.reley.messenger.message.dto;

import java.time.OffsetDateTime;


//WebSocket event
public record NewMessageDomainEvent(
        Long id,
        Long dialogId,
        Long senderId,
        String senderName,
        String senderEmail,
        String content,
        OffsetDateTime createdAt
) {

    public static NewMessageDomainEvent fromMessageResponse(MessageResponse message, MessageResponse.SenderInfo senderInfo) {
        return new NewMessageDomainEvent(
                message.id(),
                message.dialogId(),
                message.senderId(),
                senderInfo.name(),
                senderInfo.email(),
                message.content(),
                message.createdAt()
        );
    }

}
