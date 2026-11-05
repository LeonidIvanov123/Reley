package com.reley.messenger.user.controller;

import com.reley.messenger.user.dto.CreateUserRequest;
import com.reley.messenger.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
@Deprecated
public class RegistrationController_old {

    private final UserService userService;

    @GetMapping("/register_old")
    public Mono<String> registerPage() {
        return Mono.just("register");
    }

    @PostMapping("/register_old")
    public Mono<String> register(
            @ModelAttribute CreateUserRequest request) {

        return userService.createUser(request)
                .thenReturn("redirect:/login");
    }

}
