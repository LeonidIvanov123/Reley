package com.reley.messenger.dialog.controller;

import com.reley.messenger.dialog.dto.AddDialogParticipantRequest;
import com.reley.messenger.dialog.dto.CreateDialogRequest;
import com.reley.messenger.dialog.dto.CreateGroupRequest;
import com.reley.messenger.dialog.dto.DialogResponse;
import com.reley.messenger.dialog.dto.UpdateGroupRequest;
import com.reley.messenger.dialog.service.DialogService;
import com.reley.messenger.user.service.HydrateUserInfoApi;
import com.reley.messenger.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @GetMapping("/groups/new")
    public Mono<String> newGroup(Model model) {
        model.addAttribute("group", new CreateGroupRequest(""));
        return Mono.just("group-new");
    }

    @GetMapping("/{dialogId}/settings")
    public Mono<String> groupSettings(
            @PathVariable Long dialogId,
            Model model
    ) {
        return dialogService
                .getGroupSettings(dialogId)
                .doOnNext(group -> model.addAttribute("group", group))
                .thenReturn("group-settings");
    }

    @PutMapping("/{dialogId}")
    public Mono<Void> updateGroup(
            @PathVariable Long dialogId,
            @RequestBody UpdateGroupRequest request
    ) {
        return dialogService.updateGroup(
                dialogId,
                request.name(),
                request.isPublic()
        );
    }

    @DeleteMapping("/{dialogId}/participants/{userId}")
    public Mono<Void> removeParticipant(
            @PathVariable Long dialogId,
            @PathVariable Long userId
    ) {
        return dialogService.removeParticipant(
                dialogId,
                userId
        );
    }

    @PostMapping("/{dialogId}/participants/{userId}/admin")
    public Mono<Void> makeAdmin(
            @PathVariable Long dialogId,
            @PathVariable Long userId
    ) {
        return dialogService.makeAdmin(
                dialogId,
                userId
        );
    }

    @DeleteMapping("/{dialogId}/participants/{userId}/admin")
    public Mono<Void> removeAdmin(
            @PathVariable Long dialogId,
            @PathVariable Long userId
    ) {
        return dialogService.removeAdmin(
                dialogId,
                userId
        );
    }

    @PostMapping("/{dialogId}/leave")
    public Mono<Void> leaveGroup(
            @PathVariable Long dialogId
    ) {
        return dialogService.leaveGroup(dialogId);
    }

    @PostMapping("/groups")
    public Mono<String> createGroup(
            @ModelAttribute CreateGroupRequest request
    ) {
        return dialogService
                .createGroup(request.name())
                .thenReturn("redirect:/");
    }

    @PostMapping("/{dialogId}/participants")
    public Mono<Void> addParticipant(
            @PathVariable Long dialogId,
            @RequestBody AddDialogParticipantRequest request
    ) {
        return dialogService.addParticipant(dialogId, request.userId());
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
