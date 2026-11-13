package com.reley.messenger.security;

import com.reley.messenger.dialog.repository.DialogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service("dialogAccess")
@RequiredArgsConstructor
public class DialogAccessService {

    private final DialogRepository dialogRepository;

    public Mono<Boolean> canAccess(Long dialogId, Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just(false);
        }

        Long userId;

        try {
            userId = Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            return Mono.just(false);
        }

        return dialogRepository.existsByIdAndUserId(dialogId, userId);
    }

    public Mono<Boolean> canAccess(Long dialogId, Long userId) {

        return dialogRepository.existsByIdAndUserId(dialogId, userId);
    }

}