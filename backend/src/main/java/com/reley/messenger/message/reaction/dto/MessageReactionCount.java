package com.reley.messenger.message.reaction.dto;

public record MessageReactionCount(
        Long messageId,
        String reaction,
        long count,
        boolean reactedByMe
) {
}
