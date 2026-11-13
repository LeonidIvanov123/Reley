package com.reley.messenger.message.repository;

import com.reley.messenger.attachment.entity.Attachment;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MessageAttachmentRepository {

    private final DatabaseClient databaseClient;

    public Mono<Void> insert(UUID attachmentId, Long dialogId, Long messageId) {

        return databaseClient.sql("""
                INSERT INTO message_attachment (attachment_id, dialog_id, message_id)
                VALUES (:attachmentId, :dialogId, :messageId)
                """)
                .bind("attachmentId", attachmentId)
                .bind("dialogId", dialogId)
                .bind("messageId", messageId)
                .fetch()
                .rowsUpdated()
                .then();
    }

    public Flux<Attachment> findAttachmentsByMessageId(Long messageId) {
        return databaseClient.sql("""
            SELECT fa.*
            FROM file_attachment fa
            JOIN message_attachment ma
                ON ma.attachment_id = fa.id
            WHERE ma.message_id = :messageId
            ORDER BY fa.created_at
            """)
                .bind("messageId", messageId)
                .map((row, metadata) ->
                        Attachment.builder()
                                .id(row.get("id", UUID.class))
                                .createdBy(row.get("created_by", Long.class))
                                .originalFileName(row.get("original_file_name", String.class))
                                .extension(row.get("extension", String.class))
                                .contentType(row.get("content_type", String.class))
                                .size(row.get("size", Long.class))
                                .storageKey(row.get("storage_key", String.class))
                                .createdAt(row.get("created_at", OffsetDateTime.class))
                                .build())
                .all();
    }

    public Flux<Attachment> findAttachmentsByDialogId(Long dialogId) {

        return databaseClient.sql("""
            SELECT DISTINCT fa.*
            FROM file_attachment fa
            JOIN message_attachment ma
                ON ma.attachment_id = fa.id
            WHERE ma.dialog_id = :dialogId
            ORDER BY fa.created_at DESC
            """)
                .bind("dialogId", dialogId)
                .map((row, metadata) ->
                        Attachment.builder()
                                .id(row.get("id", UUID.class))
                                .createdBy(row.get("created_by", Long.class))
                                .originalFileName(row.get("original_file_name", String.class))
                                .extension(row.get("extension", String.class))
                                .contentType(row.get("content_type", String.class))
                                .size(row.get("size", Long.class))
                                .storageKey(row.get("storage_key", String.class))
                                .createdAt(row.get("created_at", OffsetDateTime.class))
                                .build()
                )
                .all();
    }

    public Flux<MessageAttachment> findAttachmentsByMessageIds(List<Long> messageIds) {
        return databaseClient.sql("""
        SELECT
            fa.*,
            ma.message_id
        FROM file_attachment fa
        JOIN message_attachment ma
            ON ma.attachment_id = fa.id
        WHERE ma.message_id IN (:messageIds)
        ORDER BY ma.message_id, fa.created_at
        """)
                .bind("messageIds", messageIds)
                .map((row, metadata) ->
                        new MessageAttachment(
                                row.get("message_id", Long.class),
                                Attachment.builder()
                                        .id(row.get("id", UUID.class))
                                        .createdBy(row.get("created_by", Long.class))
                                        .originalFileName(row.get("original_file_name", String.class))
                                        .extension(row.get("extension", String.class))
                                        .contentType(row.get("content_type", String.class))
                                        .size(row.get("size", Long.class))
                                        .storageKey(row.get("storage_key", String.class))
                                        .createdAt(row.get("created_at", OffsetDateTime.class))
                                        .build()
                        )
                )
                .all();
    }

    public record MessageAttachment(
            Long messageId,
            Attachment attachment
    ) {}

}
