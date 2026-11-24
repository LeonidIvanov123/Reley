package com.reley.messenger.tg.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile")
public class TgController {

    @GetMapping("/tg")
    public String page() {
        return "tg";
    }
}
