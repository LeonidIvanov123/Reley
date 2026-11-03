package com.reley.messenger.dialog.controller;

import com.reley.messenger.dialog.service.DialogService;
import com.reley.messenger.message.service.MessageService;
import com.reley.messenger.user.service.HydrateUserInfoApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class DialogPageController {

    private final DialogService dialogService;
    private final MessageService messageService;
    private final HydrateUserInfoApi userServiceInfo;

    @GetMapping("/dialogs/{dialogId}")
    public Mono<String> openDialog(@PathVariable Long dialogId, Model model) {

        return userServiceInfo.getCurrentUserId()
                .flatMap(userId ->
                        dialogService.findById(dialogId)
                                .flatMap(dialog -> {
                                    model.addAttribute("dialog", dialog);
                                    model.addAttribute("currentUserId", userId);

                                    return messageService
                                            .getMessages(dialogId, null)
                                            .collectList()
                                            .doOnNext(messages ->
                                                    model.addAttribute("messages", messages)
                                            )
                                            .thenReturn("dialog");
                                })
                );
    }

}
