package com.reley.messenger.tg.controller;

import com.reley.messenger.tg.service.TgAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class TgController {

    private final Optional<TgAuthService> tgAuthService;

    @GetMapping("/profile/tg")
    public String page(Model model) {

        model.addAttribute(
                "telegramEnabled",
                tgAuthService.isPresent()
        );

        return "tg";
    }

}
