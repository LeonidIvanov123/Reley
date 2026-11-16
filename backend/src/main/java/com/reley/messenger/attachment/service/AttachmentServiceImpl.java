package com.reley.messenger.attachment.service;

import com.reley.messenger.attachment.dto.AttachmentResponse;
import com.reley.messenger.attachment.entity.Attachment;
import com.reley.messenger.attachment.repository.AttachmentRepository;
import com.reley.messenger.file.FileService;
import com.reley.messenger.user.service.HydrateUserInfoApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024L;

    private final AttachmentRepository attachmentRepository;
    private final FileService fileService;
    private final HydrateUserInfoApi userServiceInfo;

    @Override
    public Mono<AttachmentResponse> upload(FilePart file) {

        String fileName = file.filename();

        String extension = extractExtension(fileName);

        UUID attachmentId = UUID.randomUUID();
        UUID fileUuid = UUID.randomUUID();

        return userServiceInfo.getCurrentUser()
                .flatMap(user -> {

                    Attachment attachment =
                            Attachment.builder()
                                    .id(attachmentId)
                                    .fileUuid(fileUuid)
                                    .createdBy(user.userId())
                                    .originalFileName(fileName)
                                    .extension(extension)
                                    .contentType(
                                            file.headers().getContentType() != null
                                                    ? file.headers()
                                                    .getContentType()
                                                    .toString()
                                                    : "application/octet-stream"
                                    )
                                    .size(file.headers().getContentLength())
                                    .storageKey(fileUuid.toString())
                                    .createdAt(OffsetDateTime.now())
                                    .build();

                    return fileService.save(fileUuid, file.content())
                            .then(
                                    attachmentRepository.insert(
                                            attachment.getId(),
                                            attachment.getFileUuid(),
                                            attachment.getCreatedBy(),
                                            attachment.getOriginalFileName(),
                                            attachment.getExtension(),
                                            attachment.getContentType(),
                                            attachment.getSize(),
                                            attachment.getStorageKey(),
                                            attachment.getCreatedAt())
                            ).doOnError(error ->
                                    log.error(
                                            "Failed to save attachment: id={}, fileUuid={}, userId={}",
                                            attachmentId,
                                            fileUuid,
                                            user.userId(),
                                            error
                                    )
                            );
                })
                .map(AttachmentResponse::fromEntity);
    }

    @Override
    public Mono<AttachmentResponse> findById(UUID id) {

        return attachmentRepository
                .findById(id)
                .map(AttachmentResponse::fromEntity)
                .switchIfEmpty(
                        Mono.error(
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found")
                        )
                );
    }

    @Override
    public Mono<Resource> load(UUID id) {

        return attachmentRepository.findById(id)
                .flatMap(attachment ->
                        fileService.load(UUID.fromString(attachment.getStorageKey()))
                );
    }

    @Override
    public Mono<Void> delete(UUID attachmentId) {

        return attachmentRepository
                .findById(attachmentId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"))
                )
                .flatMap(attachment ->
                        attachmentRepository
                                .deleteById(attachment.getId())
                                .then(attachmentRepository.existsByFileUuid(attachment.getFileUuid()))
                                .flatMap(fileStillExists -> {

                                    if (fileStillExists) {
                                        return Mono.empty();
                                    }

                                    return fileService.delete(attachment.getFileUuid());
                                })
                );
    }

    @Override
    public Mono<AttachmentResponse> saveToMyFiles(UUID attachmentId) {

        return userServiceInfo.getCurrentUserId()
                .flatMap(
                        userId ->
                                attachmentRepository
                                        .findById(attachmentId)
                                        .switchIfEmpty(
                                                Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"))
                                        )
                                        .flatMap(
                                                source ->
                                                        attachmentRepository
                                                                .findByFileUuidAndCreatedBy(source.getFileUuid(), userId)
                                                                .switchIfEmpty(copyForUser(source, userId))
                                        )
                )
                .map(AttachmentResponse::fromEntity);
    }

    private Mono<Attachment> copyForUser(
            Attachment source,
            Long userId
    ) {
        UUID newAttachmentId = UUID.randomUUID();

        Attachment copy = Attachment.builder()
                .id(newAttachmentId)
                .fileUuid(source.getFileUuid())
                .originalFileName(source.getOriginalFileName())
                .extension(source.getExtension())
                .contentType(source.getContentType())
                .size(source.getSize())
                .storageKey(source.getFileUuid().toString())
                .createdAt(OffsetDateTime.now())
                .createdBy(userId)
                .build();

        return attachmentRepository
                .insert(
                        copy.getId(),
                        copy.getFileUuid(),
                        copy.getCreatedBy(),
                        copy.getOriginalFileName(),
                        copy.getExtension(),
                        copy.getContentType(),
                        copy.getSize(),
                        copy.getStorageKey(),
                        copy.getCreatedAt()
                )
                .thenReturn(copy);
    }

    @Override
    public Flux<AttachmentResponse> findMyAttachments() {

        return userServiceInfo.getCurrentUser()
                .flatMapMany(user ->
                        attachmentRepository
                                .findAllByCreatedBy(user.userId())
                )
                .map(AttachmentResponse::fromEntity);
    }

    private String extractExtension(String fileName) {

        int index = fileName.lastIndexOf('.');

        if (index <= 0 || index == fileName.length() - 1) {
            return null;
        }

        return fileName
                .substring(index + 1)
                .toLowerCase();
    }
}
