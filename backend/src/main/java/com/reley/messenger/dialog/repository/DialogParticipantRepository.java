package com.reley.messenger.dialog.repository;

import com.reley.messenger.dialog.entity.DialogParticipant;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.stereotype.Repository;
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
        SELECT dp.dialog_id
        FROM dialog_participants dp
        JOIN dialogs d
          ON d.id = dp.dialog_id
        WHERE dp.user_id = :userId
          AND d.dialog_name = 'Избранное'
        LIMIT 1
        """)
    Mono<Long> findFavoritesDialogId(Long userId);
}
