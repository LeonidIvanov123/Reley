package com.reley.messenger.tg.controller;

import com.reley.messenger.tg.dto.TgDialogResponse;
import com.reley.messenger.tg.dto.TgMessageResponse;
import com.reley.messenger.tg.dto.TgSelectedDialogsRequest;
import com.reley.messenger.tg.dto.TgSelectedDialogsResponse;
import com.reley.messenger.tg.service.TgChatService;
import com.reley.messenger.user.service.HydrateUserInfoApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tg")
public class TgChatController {

    private final Optional<TgChatService> tgChatService;

    private final HydrateUserInfoApi userServiceInfo;

    @GetMapping("/dialogs")
    public Mono<List<TgDialogResponse>> getDialogs() {

        if (tgChatService.isEmpty()) {
            return Mono.just(List.of());
        }

        return userServiceInfo
                .getCurrentUserId()
                .flatMap(tgChatService.get()::getSelectedDialogs);

    }

    /**
     * Все доступные неархивные Telegram-чаты.
     * Используется в настройках выбора чатов.
     */
    @GetMapping("/dialogs/available")
    public Mono<List<TgDialogResponse>> getAvailableDialogs() {

        if (tgChatService.isEmpty()) {
            return Mono.just(List.of());
        }

        return userServiceInfo
                .getCurrentUserId()
                .flatMap(tgChatService.get()::getAvailableDialogs);
    }

    /**
     * ID Telegram-чатов, выбранных пользователем.
     */
    @GetMapping("/dialogs/selected")
    public Mono<TgSelectedDialogsResponse> getSelectedDialogs() {

        if (tgChatService.isEmpty()) {
            return Mono.just(
                    new TgSelectedDialogsResponse(List.of())
            );
        }

        return userServiceInfo
                .getCurrentUserId()
                .flatMap(tgChatService.get()::getSelectedDialogIds)
                .map(TgSelectedDialogsResponse::new);
    }

    /**
     * Сохраняет выбранные Telegram-чаты.
     */
    @PutMapping("/dialogs/selected")
    public Mono<Void> saveSelectedDialogs(
            @RequestBody TgSelectedDialogsRequest request
    ) {

        if (tgChatService.isEmpty()) {
            return Mono.empty();
        }

        List<Long> chatIds =
                request == null || request.chatIds() == null
                        ? List.of()
                        : request.chatIds();

        return userServiceInfo
                .getCurrentUserId()
                .flatMap(userId ->
                        tgChatService.get()
                                .saveSelectedDialogs(userId, chatIds)
                );
    }

    @GetMapping("/dialogs/{chatId}/messages")
    public Mono<List<TgMessageResponse>> getMessages(
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) Long fromMessageId
    ) {
        if (tgChatService.isEmpty()) {
            return Mono.just(List.of());
        }

        int actualLimit = Math.min(Math.max(limit, 1), 100);

        return userServiceInfo
                .getCurrentUserId()
                .flatMap(userId ->
                        tgChatService.get()
                                .getMessages(
                                        userId,
                                        chatId,
                                        actualLimit,
                                        fromMessageId
                                )
                );
    }


}
