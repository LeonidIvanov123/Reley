package com.reley.messenger.message.dto;

import java.time.OffsetDateTime;


//WebSocket event
public record NewMessageDomainEvent(
        Long id,
        Long dialogId,
        Long senderId,
        String content,
        OffsetDateTime createdAt
) {

    public static NewMessageDomainEvent fromMessageResponse(MessageResponse message) {
        return new NewMessageDomainEvent(
                message.id(),
                message.dialogId(),
                message.senderId(),
                message.content(),
                message.createdAt()
        );
    }

}
