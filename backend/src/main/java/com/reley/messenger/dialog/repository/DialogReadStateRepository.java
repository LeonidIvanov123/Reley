package com.reley.messenger.dialog.repository;

import com.reley.messenger.dialog.entity.DialogReadState;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Repository
public interface DialogReadStateRepository extends ReactiveCrudRepository<DialogReadState, Long> {

    Mono<DialogReadState> findByDialogIdAndUserId(Long dialogId, Long userId);

    @Query("""
        SELECT COALESCE(COUNT(m.id), 0)
        FROM messages m
        LEFT JOIN dialog_read_state rs
               ON rs.dialog_id = m.dialog_id
              AND rs.user_id = :userId
        WHERE m.dialog_id = :dialogId AND m.sender_id <> :userId
          AND (
              rs.last_read_message_id IS NULL
              OR m.id > rs.last_read_message_id
          )
        """)
    Mono<Long> countUnreadMessages(Long dialogId, Long userId);

    @Query("""
    INSERT INTO dialog_read_state (
        dialog_id,
        user_id,
        last_read_message_id,
        updated_at
    )
    VALUES (
        :dialogId,
        :userId,
        :lastReadMessageId,
        :updatedAt
    )
    ON CONFLICT (dialog_id, user_id)
    DO UPDATE SET
        last_read_message_id = GREATEST(
            dialog_read_state.last_read_message_id,
            EXCLUDED.last_read_message_id
        ),
        updated_at = EXCLUDED.updated_at
    """)
    Mono<Integer> upsertReadState(
            Long dialogId,
            Long userId,
            Long lastReadMessageId,
            OffsetDateTime updatedAt
    );

}