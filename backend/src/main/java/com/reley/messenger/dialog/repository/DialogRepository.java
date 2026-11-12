package com.reley.messenger.dialog.repository;

import com.reley.messenger.dialog.entity.Dialog;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Repository
public interface DialogRepository extends ReactiveCrudRepository<Dialog, Long> {

    @Query("""
        SELECT d.*
        FROM dialogs d
        JOIN dialog_participants dp1
          ON dp1.dialog_id = d.id
        JOIN dialog_participants dp2
          ON dp2.dialog_id = d.id
        WHERE d.group_chat = false
          AND dp1.user_id = :userId
          AND dp2.user_id = :otherUserId
        LIMIT 1
        """)
    Mono<Dialog> findPrivateDialog(
            Long userId,
            Long otherUserId
    );

    @Query("""
        SELECT d.*
        FROM dialogs d
        JOIN dialog_participants dp
          ON dp.dialog_id = d.id
        WHERE dp.user_id = :userId
        """)
    Flux<Dialog> findAllByUserId(Long userId);

    @Query("""
        SELECT id
        FROM messages
        WHERE dialog_id = :dialogId
        ORDER BY id DESC
        LIMIT 1
        """)
    Mono<Long> findLastMessageId(Long dialogId);

    @Query("""
        UPDATE dialogs
        SET dialog_name = :name,
            is_public = :isPublic,
            dialog_link = :dialogLink,
            updated_at = :updatedAt
        WHERE id = :dialogId
          AND group_chat = true
        """)
    Mono<Void> updateGroup(
            Long dialogId,
            String name,
            boolean isPublic,
            String dialogLink,
            OffsetDateTime updatedAt
    );

    @Query("""
        SELECT *
        FROM dialogs
        WHERE id = :dialogId
          AND group_chat = true
        """)
    Mono<Dialog> findGroupById(Long dialogId);

}
