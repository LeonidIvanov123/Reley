package com.reley.messenger.websocket.event;

import java.time.OffsetDateTime;

public record DialogUpdatedEvent(
        Long dialogId,
        String dialogName,
        Boolean groupChat,
        String lastMessagePreview,
        long unreadCount,
        OffsetDateTime lastMessageAt
) {
    public DialogUpdatedEvent(Long dialogId, long unreadCount, OffsetDateTime lastMessageAt) {
        this(dialogId, null, null, null, unreadCount, lastMessageAt);
    }

    public DialogUpdatedEvent(Long dialogId, String dialogName, Boolean groupChat, String lastMessagePreview, long unreadCount, OffsetDateTime lastMessageAt) {
        this.dialogId = dialogId;
        this.dialogName = dialogName;
        this.groupChat = groupChat;
        this.lastMessagePreview = lastMessagePreview;
        this.unreadCount = unreadCount;
        this.lastMessageAt = lastMessageAt;
    }
}