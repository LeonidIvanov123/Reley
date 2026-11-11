package com.reley.messenger.message.reaction.service;

import com.reley.messenger.message.reaction.dto.MessageReactionChangedDomainEvent;
import com.reley.messenger.message.reaction.dto.MessageReactionCount;
import com.reley.messenger.message.reaction.dto.MessageReactionResponse;
import com.reley.messenger.message.reaction.repository.MessageReactionRepository;
import com.reley.messenger.message.repository.MessageRepository;
import com.reley.messenger.websocket.event.MessageReactionChangedWebSocketEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageReactionServiceImpl implements MessageReactionService {

    private static final Set<String> ALLOWED_REACTIONS =
            Set.of(
                    "👍",
                    "❤️",
                    "😂",
                    "😮",
                    "😢",
                    "😡"
            );

    private final MessageReactionRepository messageReactionRepository;
    private final MessageRepository messageRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Mono<Map<Long, List<MessageReactionResponse>>> getReactions(
            List<Long> messageIds,
            Long userId
    ) {
        if (messageIds.isEmpty()) {
            return Mono.just(Map.of());
        }

        return messageReactionRepository
                .countByMessageIds(
                        messageIds,
                        userId
                )
                .collectMultimap(
                        MessageReactionCount::messageId,
                        item -> new MessageReactionResponse(
                                item.reaction(),
                                item.count(),
                                item.reactedByMe()
                        )
                )
                .map(map ->
                        map.entrySet()
                                .stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> List.copyOf(
                                                entry.getValue()
                                        )
                                ))
                );
    }

    @Override
    public Mono<List<MessageReactionResponse>> setReaction(
            Long dialogId,
            Long messageId,
            Long userId,
            String reaction
    ) {
        validateReaction(reaction);

        return checkMessageBelongsToDialog(
                messageId,
                dialogId
        )
                .then(
                        messageReactionRepository
                                .findByMessageIdAndUserId(
                                        messageId,
                                        userId
                                )
                )
                .flatMap(existing -> {

                    String oldReaction = existing.getReaction();

                    if (oldReaction.equals(reaction)) {

                        return messageReactionRepository
                                .delete(
                                        messageId,
                                        userId
                                )
                                .thenReturn(
                                        new ReactionChange(
                                                oldReaction,
                                                null
                                        )
                                );
                    }

                    return messageReactionRepository
                            .update(
                                    messageId,
                                    userId,
                                    reaction
                            )
                            .thenReturn(
                                    new ReactionChange(
                                            oldReaction,
                                            reaction
                                    )
                            );
                })
                .switchIfEmpty(
                        Mono.defer(() ->
                                messageReactionRepository
                                        .insert(
                                                messageId,
                                                userId,
                                                reaction,
                                                OffsetDateTime.now()
                                        )
                                        .thenReturn(
                                                new ReactionChange(
                                                        null,
                                                        reaction
                                                )
                                        )
                        )
                )
                .flatMap(change ->
                        getReactions(
                                messageId,
                                userId
                        )
                                .flatMap(reactions -> {

                                    List<MessageReactionChangedWebSocketEvent.MessageReactionCountResponse>
                                            websocketReactions =
                                            reactions.stream()
                                                    .map(item ->
                                                            new MessageReactionChangedWebSocketEvent.MessageReactionCountResponse(
                                                                    item.reaction(),
                                                                    item.count()
                                                            )
                                                    )
                                                    .toList();

                                    eventPublisher.publishEvent(
                                            new MessageReactionChangedDomainEvent(
                                                    dialogId,
                                                    messageId,
                                                    userId,
                                                    change.oldReaction(),
                                                    change.newReaction(),
                                                    websocketReactions
                                            )
                                    );

                                    return Mono.just(reactions);
                                })
                );
    }

    @Override
    public Mono<List<MessageReactionResponse>> removeReaction(
            Long dialogId,
            Long messageId,
            Long userId
    ) {
        return checkMessageBelongsToDialog(
                messageId,
                dialogId
        )
                .then(
                        messageReactionRepository
                                .findByMessageIdAndUserId(
                                        messageId,
                                        userId
                                )
                )
                .flatMap(existing ->
                        messageReactionRepository
                                .delete(
                                        messageId,
                                        userId
                                )
                                .thenReturn(
                                        existing.getReaction()
                                )
                )
                .flatMap(oldReaction ->
                        getReactions(
                                messageId,
                                userId
                        )
                                .flatMap(reactions -> {

                                    List<MessageReactionChangedWebSocketEvent.MessageReactionCountResponse>
                                            websocketReactions =
                                            reactions.stream()
                                                    .map(item ->
                                                            new MessageReactionChangedWebSocketEvent.MessageReactionCountResponse(
                                                                    item.reaction(),
                                                                    item.count()
                                                            )
                                                    )
                                                    .toList();

                                    eventPublisher.publishEvent(
                                            new MessageReactionChangedDomainEvent(
                                                    dialogId,
                                                    messageId,
                                                    userId,
                                                    oldReaction,
                                                    null,
                                                    websocketReactions
                                            )
                                    );

                                    return Mono.just(reactions);
                                })
                );
    }

    private Mono<List<MessageReactionResponse>> getReactions(
            Long messageId,
            Long userId
    ) {
        return getReactions(
                List.of(messageId),
                userId
        )
                .map(reactions ->
                        reactions.getOrDefault(
                                messageId,
                                List.of()
                        )
                );
    }

    private Mono<Void> checkMessageBelongsToDialog(
            Long messageId,
            Long dialogId
    ) {
        return messageRepository
                .existsByIdAndDialogId(
                        messageId,
                        dialogId
                )
                .filter(Boolean::booleanValue)
                .switchIfEmpty(
                        Mono.error(
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Message not found"
                                )
                        )
                )
                .then();
    }

    private void validateReaction(String reaction) {
        if (reaction == null
                || !ALLOWED_REACTIONS.contains(reaction)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported reaction"
            );
        }
    }

    private record ReactionChange(
            String oldReaction,
            String newReaction
    ) {
    }

}