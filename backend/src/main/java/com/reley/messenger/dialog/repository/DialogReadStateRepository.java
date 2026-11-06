package com.reley.messenger.dialog.repository;

import com.reley.messenger.dialog.entity.DialogReadState;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

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

}