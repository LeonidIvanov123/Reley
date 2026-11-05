package com.reley.messenger.dialog.controller;

import com.reley.messenger.dialog.dto.CreateDialogRequest;
import com.reley.messenger.dialog.service.DialogService;
import com.reley.messenger.exception.CreateDialogException;
import com.reley.messenger.exception.UserNotFoundException;
import com.reley.messenger.user.repository.UserRepository;
import com.reley.messenger.user.service.HydrateUserInfoApi;
import com.reley.messenger.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
@RequestMapping("/dialogs")
public class DialogController {

    private final UserService userService;
    private final HydrateUserInfoApi userInfo;
    private final DialogService dialogService;

    @GetMapping("new")
    public Mono<String> newDialog(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", error);
        }
        return prepareNewDialogModel(model)
                .thenReturn("dialog-new");
    }

    @PostMapping("new")
    public Mono<String> createDialog(
            @ModelAttribute CreateDialogRequest request
    ) {
        return userService
                .findByEmail(request.email())
                .flatMap(user ->
                        dialogService.createDialog(user.getId())
                                .thenReturn("redirect:/")
                )
                .switchIfEmpty(
                        Mono.fromSupplier(() ->
                                "redirect:/dialogs/new?error="
                                        + UriUtils.encode(
                                                "Пользователь с email %s не найден"
                                                        .formatted(request.email()), StandardCharsets.UTF_8)
                        )
                );
    }

    private Mono<Void> prepareNewDialogModel(Model model) {

        return userInfo.getCurrentUser()
                .flatMap(currentUser ->
                        userService.getDirectoryUsers()
                                .collectList()
                                .doOnNext(users -> {
                                    model.addAttribute("users", users);
                                    model.addAttribute(
                                            "currentUserEmail",
                                            currentUser.email()
                                    );
                                })
                )
                .then();
    }

}
