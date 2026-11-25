package com.reley.messenger.tg.controller;

import com.reley.messenger.tg.service.TgChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class TgController {

    private final Optional<TgChatService> tgChatService;

    @GetMapping("/profile/tg")
    public String page(Model model) {

        model.addAttribute(
                "telegramEnabled",
                tgChatService.isPresent()
        );

        return "tg";
    }

    @GetMapping("/profile/tg/chats")
    public String chatsPage(Model model) {
        model.addAttribute(
                "telegramEnabled",
                tgChatService.isPresent()
        );

        return "tg-chats";
    }

    @GetMapping("/tg/dialogs/{chatId}")
    public Mono<String> tgDialog(@PathVariable Long chatId, Model model) {

        model.addAttribute("chatId", chatId);
        return Mono.just("tg-dialog");
    }

}
