package com.reley.messenger.message.reaction.repository;

import com.reley.messenger.message.reaction.dto.MessageReactionCount;
import com.reley.messenger.message.reaction.entity.MessageReaction;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Collection;

@Repository
@RequiredArgsConstructor
public class MessageReactionRepository {

    private final DatabaseClient databaseClient;

    public Mono<Long> insert(
            Long messageId,
            Long userId,
            String reaction,
            OffsetDateTime createdAt
    ) {
        return databaseClient.sql("""
                INSERT INTO message_reaction (
                    message_id,
                    user_id,
                    reaction,
                    created_at
                )
                VALUES (
                    :messageId,
                    :userId,
                    :reaction,
                    :createdAt
                )
                """)
                .bind("messageId", messageId)
                .bind("userId", userId)
                .bind("reaction", reaction)
                .bind("createdAt", createdAt)
                .fetch()
                .rowsUpdated();
    }

    public Mono<Long> update(
            Long messageId,
            Long userId,
            String reaction
    ) {
        return databaseClient.sql("""
                UPDATE message_reaction
                SET reaction = :reaction
                WHERE message_id = :messageId
                  AND user_id = :userId
                """)
                .bind("messageId", messageId)
                .bind("userId", userId)
                .bind("reaction", reaction)
                .fetch()
                .rowsUpdated();
    }

    public Mono<Long> delete(
            Long messageId,
            Long userId
    ) {
        return databaseClient.sql("""
                DELETE FROM message_reaction
                WHERE message_id = :messageId
                  AND user_id = :userId
                """)
                .bind("messageId", messageId)
                .bind("userId", userId)
                .fetch()
                .rowsUpdated();
    }

    public Mono<MessageReaction> findByMessageIdAndUserId(
            Long messageId,
            Long userId
    ) {
        return databaseClient.sql("""
                        SELECT
                            message_id,
                            user_id,
                            reaction,
                            created_at
                        FROM message_reaction
                        WHERE message_id = :messageId
                          AND user_id = :userId
                        """)
                .bind("messageId", messageId)
                .bind("userId", userId)
                .map((row, metadata) ->
                        MessageReaction.builder()
                                .messageId(
                                        row.get(
                                                "message_id",
                                                Long.class
                                        )
                                )
                                .userId(
                                        row.get(
                                                "user_id",
                                                Long.class
                                        )
                                )
                                .reaction(
                                        row.get(
                                                "reaction",
                                                String.class
                                        )
                                )
                                .createdAt(
                                        row.get(
                                                "created_at",
                                                OffsetDateTime.class
                                        )
                                )
                                .build()
                )
                .one();
    }

    public Flux<MessageReactionCount> countByMessageIds(Collection<Long> messageIds, Long userId) {
        if (messageIds.isEmpty()) {
            return Flux.empty();
        }

        return databaseClient.sql("""
            SELECT
                mr.message_id,
                mr.reaction,
                COUNT(*) AS reaction_count,
                BOOL_OR(mr.user_id = :userId) AS reacted_by_me
            FROM message_reaction mr
            WHERE mr.message_id IN (:messageIds)
            GROUP BY
                mr.message_id,
                mr.reaction
            ORDER BY
                mr.message_id,
                mr.reaction
            """)
                .bind("messageIds", messageIds)
                .bind("userId", userId)
                .map((row, metadata) ->
                        new MessageReactionCount(
                                row.get("message_id", Long.class),
                                row.get("reaction", String.class),
                                row.get("reaction_count", Long.class),
                                Boolean.TRUE.equals(
                                        row.get(
                                                "reacted_by_me",
                                                Boolean.class
                                        )
                                )
                        )
                )
                .all();
    }


}
