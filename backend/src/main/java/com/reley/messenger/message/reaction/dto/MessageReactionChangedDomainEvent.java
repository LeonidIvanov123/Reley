package com.reley.messenger.message.reaction.dto;

import com.reley.messenger.websocket.event.MessageReactionChangedWebSocketEvent;

import java.util.List;

public record MessageReactionChangedDomainEvent(
        Long dialogId,
        Long messageId,
        Long userId,
        String oldReaction,
        String newReaction,
        List<MessageReactionChangedWebSocketEvent.MessageReactionCountResponse> reactions
) {
}