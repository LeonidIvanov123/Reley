package com.reley.messenger.websocket.event;

import com.reley.messenger.attachment.dto.AttachmentResponse;

import java.time.OffsetDateTime;
import java.util.List;

public record NewMessageEvent(
        Long id,
        Long dialogId,
        Long senderId,
        String senderName,
        String senderEmail,
        String content,
        OffsetDateTime createdAt,
        List<AttachmentResponse> attachments
) {
}
