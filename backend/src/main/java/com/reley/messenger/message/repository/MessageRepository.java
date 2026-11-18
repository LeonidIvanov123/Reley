package com.reley.messenger.message.repository;

import com.reley.messenger.message.entity.Message;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Collection;

@Repository
public interface MessageRepository extends ReactiveCrudRepository<Message, Long> {

    Flux<Message> findTop15ByDialogIdOrderByIdDesc(Long dialogId);

    Flux<Message> findTop15ByDialogIdAndIdLessThanOrderByIdDesc(Long dialogId, Long beforeId);

    @Query("""
        SELECT *
        FROM messages
        WHERE dialog_id = :dialogId
          AND id > :afterId
        ORDER BY id ASC
        LIMIT :limit
        """)
    Flux<Message> findTopByDialogIdAndIdGreaterThanOrderByIdAsc(Long dialogId, Long afterId, int limit);

    @Modifying
    @Query("""
    UPDATE messages SET deleted_at = :deletedAt 
       WHERE id = :messageId AND dialog_id = :dialogId AND sender_id = :senderId AND deleted_at IS NULL
    """)
    Mono<Integer> markAsDeleteMessage(
            Long messageId,
            Long dialogId,
            Long senderId,
            OffsetDateTime deletedAt
    );

    @Query("SELECT MAX(created_at) FROM messages WHERE dialog_id = :dialogId")
    Mono<OffsetDateTime> findLastMessageDate(Long dialogId);

    @Query("""
        SELECT EXISTS (
            SELECT 1
            FROM messages
            WHERE id = :messageId
              AND dialog_id = :dialogId
        )
        """)
    Mono<Boolean> existsByIdAndDialogId(
            Long messageId,
            Long dialogId
    );

    @Query("""
        SELECT *
        FROM messages
        WHERE id = :messageId
          AND dialog_id = :dialogId
        """)
    Mono<Message> findByIdAndDialogId(
            Long messageId,
            Long dialogId
    );

    @Query("""
        SELECT COUNT(*)
        FROM messages
        WHERE dialog_id = :dialogId
          AND (:messageId IS NULL OR id > :messageId)
        """)
    Mono<Long> countMessagesAfterId(
            Long dialogId,
            Long messageId
    );

    @Query("""
        SELECT id
        FROM messages
        WHERE dialog_id = :dialogId
        ORDER BY id DESC
        LIMIT 1
        """)
    Mono<Long> findLastMessageId(Long dialogId);

    @Query("""
        SELECT * FROM messages
        WHERE dialog_id = :dialogId AND content ILIKE '%' || :query || '%'
        ORDER BY id DESC
        LIMIT :limit
        OFFSET :offset
        """)
    Flux<Message> search(Long dialogId, String query, int limit, int offset);

    @Query("""
        DELETE FROM messages WHERE dialog_id = :dialogId
        """)
    Mono<Void> deleteByDialogId(Long dialogId);

    @Query("""
        SELECT *
        FROM messages
        WHERE id IN (:messageIds)
        """)
    Flux<Message> findAllByIds(Collection<Long> messageIds);

    @Query("""
        SELECT *
        FROM messages
        WHERE dialog_id = :dialogId
          AND id < :messageId
        ORDER BY id DESC
        LIMIT :limit
        """)
    Flux<Message> findBefore(
            Long dialogId,
            Long messageId,
            int limit
    );

    @Query("""
        SELECT *
        FROM messages
        WHERE dialog_id = :dialogId
          AND id > :messageId
        ORDER BY id ASC
        LIMIT :limit
        """)
    Flux<Message> findAfter(
            Long dialogId,
            Long messageId,
            int limit
    );

}
