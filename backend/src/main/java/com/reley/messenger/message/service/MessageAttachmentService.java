package com.reley.messenger.message.service;

import com.reley.messenger.attachment.dto.AttachmentResponse;
import com.reley.messenger.attachment.entity.Attachment;
import com.reley.messenger.attachment.repository.AttachmentRepository;
import com.reley.messenger.message.repository.MessageAttachmentRepository;
import com.reley.messenger.user.service.HydrateUserInfoApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageAttachmentService {

    private final MessageAttachmentRepository messageAttachmentRepository;
    private final AttachmentRepository attachmentRepository;
    private final HydrateUserInfoApi userInfoApi;

    public Flux<AttachmentResponse> findDialogAttachments(
            Long dialogId
    ) {
        return userInfoApi.getCurrentUserId()
                .flatMapMany(currentUserId ->
                        attachmentRepository
                                .findFileUuidsByCreatedBy(currentUserId)
                                .collect(Collectors.toSet())
                                .zipWith(
                                        messageAttachmentRepository
                                                .findAttachmentsByDialogId(dialogId)
                                                .distinct(Attachment::getId)
                                                .collectList()
                                )
                                .flatMapMany(tuple -> {
                                    Set<UUID> myFileUuids = tuple.getT1();
                                    List<Attachment> attachments = tuple.getT2();

                                    return Flux.fromIterable(attachments)
                                            .map(attachment ->
                                                    AttachmentResponse.fromEntity(
                                                            attachment,
                                                            myFileUuids.contains(attachment.getFileUuid())
                                                    )
                                            );
                                })
                );
    }

    public Mono<Void> deleteByDialogId(Long dialogId) {
        return messageAttachmentRepository.deleteByDialogId(dialogId);
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
