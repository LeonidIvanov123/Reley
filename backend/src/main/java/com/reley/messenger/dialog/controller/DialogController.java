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
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
@RequestMapping("/dialogs")
public class DialogController {

    private final UserService userService;
    private final HydrateUserInfoApi userInfo;
    private final DialogService dialogService;

    @GetMapping("new")
    public Mono<String> newDialog(Model model) {

        return userInfo.getCurrentUser()
                .flatMap(currentUser ->
                        userService.getDirectoryUsers()
                                .collectList()
                                .doOnNext(users -> {
                                    model.addAttribute("users", users);
                                    model.addAttribute("currentUserEmail", currentUser.email());
                                })
                                .thenReturn("dialog-new")
                );
    }

    @PostMapping("new")
    public Mono<String> createDialog(
            @ModelAttribute CreateDialogRequest request
    ) {
        return userService.findByEmail(request.email())
                .switchIfEmpty(
                        Mono.error(new CreateDialogException("Не найден пользователь с емэилом %s\n".formatted(request.email())))
                )
                .flatMap(user ->
                        dialogService.createDialog(user.getId())
                )
                .thenReturn("redirect:/");
    }

}
