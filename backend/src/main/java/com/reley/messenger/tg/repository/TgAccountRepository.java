package com.reley.messenger.tg.repository;

import com.reley.messenger.tg.entity.TgAccount;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface TgAccountRepository extends ReactiveCrudRepository<TgAccount, Long> {

    Mono<TgAccount> findByUserId(Long userId);

    Mono<Boolean> existsByUserId(Long userId);

    Mono<TgAccount> findByTelegramUserId(Long telegramUserId);

}