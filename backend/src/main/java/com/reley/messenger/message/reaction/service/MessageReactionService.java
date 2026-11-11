package com.reley.messenger.message.reaction.service;

import com.reley.messenger.message.reaction.dto.MessageReactionResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface MessageReactionService {

    Mono<Map<Long, List<MessageReactionResponse>>> getReactions(List<Long> messageIds, Long userId);

    Mono<List<MessageReactionResponse>> setReaction(Long dialogId, Long messageId, Long userId, String reaction);

    Mono<List<MessageReactionResponse>> removeReaction(Long dialogId, Long messageId, Long userId);

}