package com.reley.messenger.user.controller;

import com.reley.messenger.dialog.service.DialogService;
import com.reley.messenger.user.service.HydrateUserInfoApi;
import com.reley.messenger.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final HydrateUserInfoApi userServiceInfo;
    private final UserService userService;
    private final DialogService dialogService;


    @GetMapping("/")
    public Mono<String> home(Model model) {
        return userServiceInfo.getCurrentUser()
                .flatMap(user -> {
                    model.addAttribute("user", user);

                    return dialogService.getMyDialogs()
                            .collectList()
                            .doOnNext(dialogs -> model.addAttribute("dialogs", dialogs))
                            .thenReturn("index");
                });
    }

}
