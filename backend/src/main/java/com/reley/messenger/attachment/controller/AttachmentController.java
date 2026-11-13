package com.reley.messenger.attachment.controller;

import com.reley.messenger.attachment.dto.AttachmentResponse;
import com.reley.messenger.attachment.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping(
            value = "/api/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseBody
    public Mono<AttachmentResponse> upload(@RequestPart("file") FilePart file) {
        return attachmentService.upload(file);
    }

    @GetMapping("/api/attachments/{id}/download")
    @ResponseBody
    public Mono<ResponseEntity<Resource>> download(@PathVariable UUID id) {
        return attachmentService.findById(id)
                .flatMap(attachment ->
                        attachmentService.load(id)
                                .map(resource -> {

                                    ResponseEntity.BodyBuilder builder =
                                            ResponseEntity.ok()
                                                    .contentType(MediaType.parseMediaType(attachment.contentType()))
                                                    .header(
                                                            HttpHeaders.CONTENT_DISPOSITION,
                                                            "attachment; filename=\""
                                                                    + attachment.fileName()
                                                                    + "\"");

                                    if (attachment.size() >= 0) {
                                        builder.contentLength(
                                                attachment.size()
                                        );
                                    }

                                    return builder.body(resource);
                                })
                );
    }

    @DeleteMapping("/api/attachments/{id}")
    @ResponseBody
    public Mono<Void> delete(@PathVariable UUID id) {
        return attachmentService.delete(id);
    }

    @GetMapping("/api/attachments")
    @ResponseBody
    public Flux<AttachmentResponse> getMyAttachments() {
        return attachmentService.findMyAttachments();
    }

    @GetMapping("/attachments")
    public Mono<String> attachmentsPage(Model model) {

        return attachmentService.findMyAttachments()
                .collectList()
                .map(attachments -> {
                    model.addAttribute("attachments", attachments);
                    return "attachments";
                });
    }
}
