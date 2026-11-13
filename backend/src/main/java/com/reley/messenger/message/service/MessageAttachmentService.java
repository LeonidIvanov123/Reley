package com.reley.messenger.message.service;

import com.reley.messenger.attachment.dto.AttachmentResponse;
import com.reley.messenger.attachment.entity.Attachment;
import com.reley.messenger.message.repository.MessageAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageAttachmentService {

    private final MessageAttachmentRepository messageAttachmentRepository;

    public Flux<AttachmentResponse> findDialogAttachments(Long dialogId) {
        return messageAttachmentRepository
                .findAttachmentsByDialogId(dialogId)
                .map(AttachmentResponse::fromEntity);
    }

    public Flux<MessageAttachmentRepository.MessageAttachment> findAttachmentsByMessageIds(List<Long> messageIds) {
        return messageAttachmentRepository.findAttachmentsByMessageIds(messageIds);
    }

    public Mono<Void> attachToMessage(UUID attachmentId, Long dialogId, Long messageId) {
        return messageAttachmentRepository.insert(attachmentId, dialogId, messageId);
    }

    public Flux<Attachment> findAttachmentsByMessageId(Long messageId) {
        return messageAttachmentRepository.findAttachmentsByMessageId(messageId);
    }

}
