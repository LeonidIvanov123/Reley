package com.reley.messenger.dialog.repository;

import com.reley.messenger.dialog.entity.DialogParticipant;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Repository
public interface DialogParticipantRepository extends org.springframework.data.repository.Repository<DialogParticipant, Void> {

    @Query("""
            SELECT dp1.dialog_id
            FROM dialog_participants dp1
            JOIN dialog_participants dp2
              ON dp1.dialog_id = dp2.dialog_id
            WHERE dp1.user_id = :userId
              AND dp2.user_id = :otherUserId
            LIMIT 1
            """)
    Mono<Long> findDialogId(
            Long userId,
            Long otherUserId
    );

    @Query("""
            INSERT INTO dialog_participants (dialog_id, user_id, joined_at)
            VALUES (:dialogId, :userId, :joinedAt)
            """)
    Mono<Void> insert(
            Long dialogId,
            Long userId,
            OffsetDateTime joinedAt
    );

    @Query("""
        INSERT INTO dialog_participants (
            dialog_id,
            user_id,
            is_admin,
            joined_at
        )
        VALUES (
            :dialogId,
            :userId,
            :isAdmin,
            :joinedAt
        )
        """)
    Mono<Void> insert(
            Long dialogId,
            Long userId,
            boolean isAdmin,
            OffsetDateTime joinedAt
    );

    @Query("""
        SELECT dp.dialog_id
        FROM dialog_participants dp
        JOIN dialogs d
          ON d.id = dp.dialog_id
        WHERE dp.user_id = :userId
          AND d.dialog_name = 'Избранное'
        LIMIT 1
        """)
    Mono<Long> findFavoritesDialogId(Long userId);

    @Query("""
        SELECT COUNT(*) > 0
        FROM dialog_participants
        WHERE dialog_id = :dialogId
          AND user_id = :userId
        """)
    Mono<Boolean> existsByDialogIdAndUserId(
            Long dialogId,
            Long userId
    );

    @Query("""
        SELECT COUNT(*)
        FROM dialog_participants
        WHERE dialog_id = :dialogId
          AND user_id = :userId
        """)
    Mono<Long> countByDialogIdAndUserId(Long dialogId, Long userId);

    @Query("""
            SELECT *
            FROM dialog_participants
            WHERE dialog_id = :dialogId
            """)
    Flux<DialogParticipant> findByDialogId(Long dialogId);

    @Query("""
            SELECT *
            FROM dialog_participants
            WHERE dialog_id = :dialogId
              AND user_id = :userId
            """)
    Mono<DialogParticipant> findByDialogIdAndUserId(
            Long dialogId,
            Long userId
    );

    @Query("""
        UPDATE dialog_participants
        SET last_read_message_id = :messageId
        WHERE dialog_id = :dialogId
          AND user_id = :userId
        """)
    Mono<Integer> updateLastReadMessageId(
            Long dialogId,
            Long userId,
            Long messageId
    );

    @Query("""
        DELETE FROM dialog_participants
        WHERE dialog_id = :dialogId
          AND user_id = :userId
        """)
    Mono<Void> delete(
            Long dialogId,
            Long userId
    );

    @Query("""
        UPDATE dialog_participants
        SET is_admin = true
        WHERE dialog_id = :dialogId
          AND user_id = :userId
        """)
    Mono<Void> makeAdmin(
            Long dialogId,
            Long userId
    );

    @Query("""
        UPDATE dialog_participants
        SET is_admin = false
        WHERE dialog_id = :dialogId
          AND user_id = :userId
        """)
    Mono<Void> removeAdmin(
            Long dialogId,
            Long userId
    );

    @Query("""
        SELECT COUNT(*)
        FROM dialog_participants
        WHERE dialog_id = :dialogId
          AND is_admin = true
        """)
    Mono<Long> countAdmins(Long dialogId);

    @Query("""
        SELECT COUNT(*) AS participant_count
        FROM dialog_participants
        WHERE dialog_id = :dialogId
          AND user_id = :userId
        """)
    Mono<Long> countParticipant(
            Long dialogId,
            Long userId
    );

}
