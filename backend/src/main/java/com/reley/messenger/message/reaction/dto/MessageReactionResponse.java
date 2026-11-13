package com.reley.messenger.message.reaction.dto;

public record MessageReactionResponse(
        String reaction,
        long count,
        boolean reactedByMe
) {
}