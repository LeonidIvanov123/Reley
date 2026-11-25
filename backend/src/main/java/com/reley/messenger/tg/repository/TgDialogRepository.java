package com.reley.messenger.tg.repository;

import com.reley.messenger.tg.entity.TgDialog;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TgDialogRepository
        extends ReactiveCrudRepository<TgDialog, Long> {

    Flux<TgDialog> findAllByUserId(Long userId);

    Mono<TgDialog> findByUserIdAndTelegramChatId(
            Long userId,
            Long telegramChatId
    );

    Mono<Boolean> existsByUserIdAndTelegramChatId(
            Long userId,
            Long telegramChatId
    );

    Mono<Void> deleteByUserIdAndTelegramChatId(
            Long userId,
            Long telegramChatId
    );

    Mono<Void> deleteAllByUserId(Long userId);

}