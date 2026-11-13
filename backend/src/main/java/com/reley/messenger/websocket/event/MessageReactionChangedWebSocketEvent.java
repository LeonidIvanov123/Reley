package com.reley.messenger.websocket.event;

import java.util.List;

public record MessageReactionChangedWebSocketEvent(
        Long messageId,
        Long userId,
        String oldReaction,
        String newReaction,
        List<MessageReactionCountResponse> reactions
) {

    public record MessageReactionCountResponse(
            String reaction,
            long count
    ) {
    }
}
