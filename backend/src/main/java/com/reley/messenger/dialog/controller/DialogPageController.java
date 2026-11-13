package com.reley.messenger.dialog.controller;

import com.reley.messenger.dialog.repository.DialogParticipantRepository;
import com.reley.messenger.dialog.service.DialogService;
import com.reley.messenger.message.service.MessageService;
import com.reley.messenger.user.service.HydrateUserInfoApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;

@Controller
@Slf4j
@RequiredArgsConstructor
public class DialogPageController {

    private final DialogService dialogService;
    private final MessageService messageService;
    private final HydrateUserInfoApi userServiceInfo;
    private final DialogParticipantRepository participantRepository;

    @PreAuthorize("@dialogAccess.canAccess(#dialogId, authentication)")
    @GetMapping("/dialogs/{dialogId}")
    public Mono<String> openDialog(
            @PathVariable Long dialogId,
            Model model
    ) {
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
                                            .then(dialogService.markAsRead(dialogId, userId))
                                            .thenReturn("dialogs/dialog");
                                })
                );
    }

    @GetMapping("/dialogs/groups/link/{link}")
    public Mono<String> openGroupByLink(
            @PathVariable String link,
            Model model
    ) {
            return userServiceInfo
                    .getCurrentUserId()
                    .flatMap(currentUserId ->
                            dialogService
                                    .getGroupByLink(link)
                                    .flatMap(group ->
                                            participantRepository
                                                    .existsByDialogIdAndUserId(
                                                            group.id(),
                                                            currentUserId
                                                    )
                                                    .flatMap(isParticipant -> {

                                                        if (isParticipant) {
                                                            return Mono.just("redirect:/dialogs/" + group.id());
                                                        }

                                                        model.addAttribute("group", group);

                                                        return Mono.just("group-invite");
                                                    })
                                    )
                    );
        }

    @PostMapping("/dialogs/groups/link/{link}/join")
    public Mono<String> joinGroup(@PathVariable String link) {
        return dialogService
                .joinGroup(link)
                .flatMap(dialogId ->
                        Mono.just(
                                "redirect:/dialogs/" + dialogId
                        )
                );
    }

}
