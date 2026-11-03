package com.reley.messenger.message.controller;

import com.reley.messenger.message.dto.MessageResponse;
import com.reley.messenger.message.dto.SendMessageRequest;
import com.reley.messenger.message.service.MessageService;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("api/dialogs/{dialogId}/messages")
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public Flux<MessageResponse> getMessages(
            @PathVariable Long dialogId,
            @RequestParam(required = false) Long beforeId
    ) {
        return messageService.getMessages(dialogId, beforeId);
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
}