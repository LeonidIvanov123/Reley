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

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tg")
public class TgAuthController {

    private final Optional<TgAuthService> tgAuthService;

    private final HydrateUserInfoApi userServiceInfo;


    @GetMapping("/auth/status")
    public Mono<TgAuthStatusResponse> getStatus() {

        if (tgAuthService.isEmpty()) {
            return Mono.just(
                    new TgAuthStatusResponse(
                            false,
                            null,
                            null,
                            null
                    )
            );
        }

        return userServiceInfo
                .getCurrentUserId()
                .flatMap(
                        tgAuthService.get()::getStatus
                );
    }


    @PostMapping("/auth/start")
    public Mono<TgAuthQrResponse> startAuth() {

        if (tgAuthService.isEmpty()) {
            return Mono.error(
                    new IllegalStateException(
                            "Telegram integration is disabled"
                    )
            );
        }

        return userServiceInfo
                .getCurrentUserId()
                .flatMap(
                        tgAuthService.get()::start
                );
    }


    @GetMapping(
            value = "/auth/qr",
            produces = MediaType.IMAGE_PNG_VALUE
    )
    public Mono<ResponseEntity<byte[]>> getQr() {

        if (tgAuthService.isEmpty()) {
            return Mono.just(
                    ResponseEntity.notFound().build()
            );
        }

        return userServiceInfo
                .getCurrentUserId()
                .flatMap(
                        tgAuthService.get()::getQr
                )
                .map(qr ->
                        ResponseEntity
                                .ok()
                                .contentType(
                                        MediaType.IMAGE_PNG
                                )
                                .body(qr)
                )
                .defaultIfEmpty(
                        ResponseEntity.notFound().build()
                );
    }


    @DeleteMapping("/auth")
    public Mono<Void> disconnect() {

        if (tgAuthService.isEmpty()) {
            return Mono.empty();
        }

        return userServiceInfo
                .getCurrentUserId()
                .flatMap(
                        tgAuthService.get()::disconnect
                );
    }


    @DeleteMapping("/auth/pending")
    public Mono<Void> cancelPendingAuth() {

        if (tgAuthService.isEmpty()) {
            return Mono.empty();
        }

        return userServiceInfo
                .getCurrentUserId()
                .flatMap(
                        tgAuthService.get()::cancelPendingAuth
                );
    }
}