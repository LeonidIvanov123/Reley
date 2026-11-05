package com.reley.messenger.user.registration.controller;

import com.reley.messenger.exception.PasswordNotMatchException;
import com.reley.messenger.user.registration.dto.RegisterRequest;
import com.reley.messenger.user.registration.dto.SetPasswordRequest;
import com.reley.messenger.user.registration.service.EmailRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class RegistrationController {

    private final EmailRegistrationService registrationService;

    @GetMapping("/register")
    public Mono<String> register(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Boolean codeSent,
            @RequestParam(required = false) Boolean verified,
            Model model) {

        model.addAttribute("email", email);
        model.addAttribute("codeSent", Boolean.TRUE.equals(codeSent));
        model.addAttribute("verified", Boolean.TRUE.equals(verified));

        return Mono.just("register");
    }

    @PostMapping("/register/code")
    public Mono<String> sendCode(
            @ModelAttribute RegisterRequest request) {

        return registrationService
                .sendVerificationCode(request.email())
                .thenReturn("redirect:/register?codeSent=true&email=" + request.email());
    }

    @PostMapping("/register/verify")
    public Mono<String> verifyCode(
            @ModelAttribute RegisterRequest request) {

        return registrationService
                .verifyCode(request.email(), request.code())
                .thenReturn("redirect:/register?verified=true&email=" + request.email());
    }

    @GetMapping("/register/verify")
    public Mono<String> verifyByLink(
            @RequestParam String email,
            @RequestParam String code) {

        return registrationService
                .verifyCode(email, code)
                .thenReturn("redirect:/register?verified=true&email=" + email);
    }

    @PostMapping("/register/password")
    public Mono<String> setPassword(
            @ModelAttribute SetPasswordRequest request
    ) {
        if (!request.password().equals(
                request.passwordConfirmation()
        )) {
            return Mono.error(new PasswordNotMatchException(request.email()));
        }

        return registrationService
                .setPassword(request.email(), request.password())
                .thenReturn("redirect:/login");
    }

}
