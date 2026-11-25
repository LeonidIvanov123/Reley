package com.reley.messenger.tg.controller;

import com.reley.messenger.tg.dto.TgDialogResponse;
import com.reley.messenger.tg.service.TgAuthService;
import com.reley.messenger.user.service.HydrateUserInfoApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tg")
public class TgDataController {

    private final Optional<TgAuthService> tgAuthService;

    private final HydrateUserInfoApi userServiceInfo;

    @GetMapping("/dialogs")
    public Mono<List<TgDialogResponse>> getDialogs() {

        if (tgAuthService.isEmpty()) {
            return Mono.just(List.of());
        }

        return userServiceInfo
                .getCurrentUserId()
                .flatMap(tgAuthService.get()::getDialogs);
    }
}
