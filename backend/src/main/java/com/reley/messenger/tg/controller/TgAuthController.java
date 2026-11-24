package com.reley.messenger.tg.controller;

import com.reley.messenger.tg.dto.TgAuthQrResponse;
import com.reley.messenger.tg.dto.TgAuthStatusResponse;
import com.reley.messenger.tg.service.TgAuthService;
import com.reley.messenger.user.service.HydrateUserInfoApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tg")
public class TgAuthController {

    private final TgAuthService tgAuthService;
    private final HydrateUserInfoApi userServiceInfo;

    @GetMapping("/auth/status")
    public Mono<TgAuthStatusResponse> getStatus() {

        return userServiceInfo
                .getCurrentUserId()
                .flatMap(tgAuthService::getStatus);
    }

    @PostMapping("/auth/start")
    public Mono<TgAuthQrResponse> startAuth() {

        return userServiceInfo
                .getCurrentUserId()
                .flatMap(tgAuthService::start);
    }

    @GetMapping(
            value = "/auth/qr",
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public Mono<ResponseEntity<byte[]>> getQr() {

        return userServiceInfo
                .getCurrentUserId()
                .flatMap(tgAuthService::getQr)
                .map(qr ->
                        ResponseEntity
                                .ok()
                                .contentType(
                                        MediaType.TEXT_PLAIN
                                )
                                .body(qr)
                )
                .defaultIfEmpty(
                        ResponseEntity.notFound().build()
                );
    }

    @DeleteMapping("/auth")
    public Mono<Void> disconnect() {
        return userServiceInfo
                .getCurrentUserId()
                .flatMap(tgAuthService::disconnect);
    }

    @DeleteMapping("/auth/pending")
    public Mono<Void> cancelPendingAuth() {
        return userServiceInfo
                .getCurrentUserId()
                .flatMap(tgAuthService::cancelPendingAuth);
    }

}