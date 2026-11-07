package com.reley.messenger.attachment.service;

import com.reley.messenger.attachment.dto.AttachmentResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AttachmentService {

    Mono<AttachmentResponse> upload(FilePart file);

    Mono<AttachmentResponse> findById(UUID id);

    Mono<Resource> load(UUID id);

    Mono<Void> delete(UUID id);

    Flux<AttachmentResponse> findMyAttachments();
}
