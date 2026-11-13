package com.reley.messenger.message.controller;

import com.reley.messenger.attachment.dto.AttachmentResponse;
import com.reley.messenger.message.service.MessageAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class MessageAttachmentController {

    private final MessageAttachmentService messageAttachmentService;

    @GetMapping("/api/dialogs/{dialogId}/attachments")
    @PreAuthorize("@dialogAccess.canAccess(#dialogId, authentication)")
    @ResponseBody
    public Flux<AttachmentResponse> dialogAttachments(@PathVariable Long dialogId) {
        return messageAttachmentService.findDialogAttachments(dialogId);
    }
}
