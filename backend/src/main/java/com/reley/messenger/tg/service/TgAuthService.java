package com.reley.messenger.tg.service;

import com.reley.messenger.tg.dto.TgAuthQrResponse;
import com.reley.messenger.tg.dto.TgAuthStatusResponse;
import com.reley.messenger.tg.dto.TgDialogResponse;
import it.tdlight.client.SimpleTelegramClient;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TgAuthService {

    Mono<TgAuthQrResponse> start(Long userId);

    Mono<Void> disconnect(Long userId);

    Mono<Void> cancelPendingAuth(Long userId);

    Mono<byte[]> getQr(Long userId);

    Mono<TgAuthStatusResponse> getStatus(Long userId);

    Mono<Boolean> isAuthCurrentUser();

    Mono<SimpleTelegramClient> getOrRestoreClient(Long userId);

}