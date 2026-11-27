package com.reley.messenger.tg.service;

import it.tdlight.client.SimpleTelegramClient;
import reactor.core.publisher.Mono;

public interface TgClientService {

    Mono<SimpleTelegramClient> getOrRestoreClient(Long userId);
}
