package com.reley.messenger.message.repository;

import com.reley.messenger.message.entity.Message;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Repository
public interface MessageRepository extends ReactiveCrudRepository<Message, Long> {

    Mono<Message> findTopByDialogIdOrderByIdDesc(Long dialogId);

    Flux<Message> findTop15ByDialogIdOrderByIdDesc(Long dialogId);

    Flux<Message> findTop15ByDialogIdAndIdLessThanOrderByIdDesc(Long dialogId, Long beforeId);

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
    SELECT *
    FROM messages WHERE dialog_id = :dialogId AND sender_id <> :userId
    ORDER BY id DESC LIMIT 1
    """)
    Mono<Message> findLastIncomingMessage(
            Long dialogId,
            Long userId
    );

}
