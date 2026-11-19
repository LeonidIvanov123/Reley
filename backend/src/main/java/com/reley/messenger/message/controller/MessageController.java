package com.reley.messenger.message.controller;

import com.reley.messenger.message.dto.MessageResponse;
import com.reley.messenger.message.dto.SendMessageRequest;
import com.reley.messenger.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/dialogs/{dialogId}/messages")
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    @PreAuthorize("@dialogAccess.canAccess(#dialogId, authentication)")
    public Flux<MessageResponse> getMessages(
            @PathVariable Long dialogId,
            @RequestParam(required = false) Long beforeId
    ) {
        return messageService.getMessages(dialogId, beforeId);
    }

    @GetMapping("/after")
    @PreAuthorize("@dialogAccess.canAccess(#dialogId, authentication)")
    public Mono<List<MessageResponse>> getMessagesAfter(@PathVariable Long dialogId, @RequestParam Long afterId) {
        return messageService
                .getMessagesAfter(dialogId, afterId)
                .collectList();
    }

    @GetMapping("/around/{messageId}")
    @PreAuthorize("@dialogAccess.canAccess(#dialogId, authentication)")
    public Mono<List<MessageResponse>> getMessageContext(
            @PathVariable Long dialogId,
            @PathVariable Long messageId,
            @RequestParam(defaultValue = "10") int before,
            @RequestParam(defaultValue = "10") int after
    ) {
        return messageService
                .getMessageContext(dialogId, messageId, before, after)
                .collectList();
    }

    @PostMapping
    public Mono<MessageResponse> sendMessage(
            @PathVariable Long dialogId,
            @RequestBody SendMessageRequest request
    ) {
        return messageService.sendMessage(dialogId, request);
    }

    @DeleteMapping("/{messageId}")
    public Mono<MessageResponse> deleteMessage(
            @PathVariable Long dialogId,
            @PathVariable Long messageId
    ) {
        return messageService.deleteMessage(dialogId, messageId);
    }

    @GetMapping("/search")
    @PreAuthorize("@dialogAccess.canAccess(#dialogId, authentication)")
    public Mono<List<MessageResponse>> searchMessages(
            @PathVariable Long dialogId,
            @RequestParam String q,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return messageService
                .searchMessages(dialogId, q, limit, offset)
                .collectList();
    }
}