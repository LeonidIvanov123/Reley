package com.reley.messenger.dialog.repository;

import com.reley.messenger.dialog.entity.Dialog;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface DialogRepository extends ReactiveCrudRepository<Dialog, Long> {

    Flux<Dialog> findAllByOwnerIdOrParticipantIdAndGroupChatFalse(Long ownerId, Long participantId);

    @Query("""
        SELECT *
        FROM dialogs
        WHERE group_chat = false
          AND ((owner_id = :userId AND participant_id = :otherUserId) OR (owner_id = :otherUserId AND participant_id = :userId))
        """)
    Mono<Dialog> findPrivateDialog(
            Long userId,
            Long otherUserId
    );
}
