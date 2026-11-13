package com.reley.messenger.message.reaction.controller;

import com.reley.messenger.message.reaction.dto.MessageReactionResponse;
import com.reley.messenger.message.reaction.dto.SetMessageReactionRequest;
import com.reley.messenger.message.reaction.service.MessageReactionService;
import com.reley.messenger.user.service.HydrateUserInfoApi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MessageReactionController {

    private final MessageReactionService messageReactionService;
    private final HydrateUserInfoApi userServiceInfo;

    @PutMapping("/api/dialogs/{dialogId}/messages/{messageId}/reaction")
    @ResponseBody
    @PreAuthorize("@dialogAccess.canAccess(#dialogId, authentication)")
    public Mono<List<MessageReactionResponse>> setReaction(@PathVariable Long dialogId,
                                                           @PathVariable Long messageId,
                                                           @RequestBody SetMessageReactionRequest request) {

        return userServiceInfo
                .getCurrentUser()
                .flatMap(user ->
                        messageReactionService.setReaction(
                                dialogId,
                                messageId,
                                user.userId(),
                                request.reaction()
                        )
                );
    }

    @DeleteMapping("/api/dialogs/{dialogId}/messages/{messageId}/reaction")
    @ResponseBody
    @PreAuthorize("@dialogAccess.canAccess(#dialogId, authentication)")
    public Mono<List<MessageReactionResponse>> removeReaction(@PathVariable Long dialogId,
                                                              @PathVariable Long messageId) {
        return userServiceInfo
                .getCurrentUser()
                .flatMap(user ->
                        messageReactionService.removeReaction(
                                dialogId,
                                messageId,
                                user.userId()
                        )
                );
    }
}
