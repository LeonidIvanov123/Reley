package com.reley.messenger.attachment.repository;

import com.reley.messenger.attachment.entity.Attachment;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends ReactiveCrudRepository<Attachment, UUID> {

    Flux<Attachment> findAllByCreatedBy(Long createdBy);

    @Query("""
        INSERT INTO file_attachment (id, file_uuid, created_by, original_file_name, extension,
                                     content_type, size, storage_key, created_at)
        VALUES (:id, :fileUuid, :createdBy, :originalFileName, :extension,
                :contentType, :size, :storageKey, :createdAt)
        RETURNING *
        """)
    Mono<Attachment> insert(UUID id, UUID fileUuid, Long createdBy, String originalFileName,
                            String extension, String contentType, Long size,
                            String storageKey, OffsetDateTime createdAt);

    @Query("""
            SELECT EXISTS (SELECT 1 FROM file_attachment WHERE file_uuid = :fileUuid)
            """)
    Mono<Boolean> existsByFileUuid(UUID fileUuid);

    @Query("""
            SELECT * FROM file_attachment
            WHERE file_uuid = :fileUuid AND created_by = :createdBy LIMIT 1
            """)
    Mono<Attachment> findByFileUuidAndCreatedBy(UUID fileUuid, Long createdBy);

    @Query("""
        SELECT file_uuid
        FROM file_attachment
        WHERE created_by = :createdBy
        """)
    Flux<UUID> findFileUuidsByCreatedBy(Long createdBy);

}