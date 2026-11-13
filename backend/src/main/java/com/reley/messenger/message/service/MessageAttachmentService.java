package com.reley.messenger.message.service;

import com.reley.messenger.attachment.dto.AttachmentResponse;
import com.reley.messenger.message.repository.MessageAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class MessageAttachmentService {

    private final MessageAttachmentRepository messageAttachmentRepository;

    public Flux<AttachmentResponse> findDialogAttachments(Long dialogId) {
        return messageAttachmentRepository
                .findAttachmentsByDialogId(dialogId)
                .map(AttachmentResponse::fromEntity);
    }

}
