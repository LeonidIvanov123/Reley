package com.reley.messenger.message.controller;

import com.reley.messenger.attachment.dto.AttachmentResponse;
import com.reley.messenger.message.service.MessageAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class MessageAttachmentController {

    private final MessageAttachmentService messageAttachmentService;

    @GetMapping("/api/dialogs/{dialogId}/attachments")
    @PreAuthorize("@dialogAccess.canAccess(#dialogId, authentication)")
    @ResponseBody
    public Flux<AttachmentResponse> dialogAttachments(@PathVariable Long dialogId) {
        return messageAttachmentService.findDialogAttachments(dialogId);
    }

    @GetMapping("/dialogs/{dialogId}/attachments")
    @PreAuthorize("@dialogAccess.canAccess(#dialogId, authentication)")
    public Mono<String> dialogAttachments(@PathVariable Long dialogId, Model model) {

        model.addAttribute("dialogId", dialogId);

        return messageAttachmentService
                .findDialogAttachments(dialogId)
                .collectList()
                .doOnNext(attachments ->
                        model.addAttribute("attachments", attachments)
                )
                .thenReturn("dialogs/dialog-attachments");
    }
}
