package com.reley.messenger.configuration.access;

import com.reley.messenger.dialog.repository.DialogParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service("dialogAccess")
@RequiredArgsConstructor
public class DialogAccessService {

    private final DialogParticipantRepository participantRepository;

    public Mono<Boolean> canAccess(
            Long dialogId,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("canAccess: authentication is null or not authenticated");
            return Mono.just(false);
        }

        Long userId;

        try {
            userId = Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            log.error(
                    "canAccess: cannot parse authentication name: {}",
                    authentication.getName(),
                    e
            );
            return Mono.just(false);
        }

        return participantRepository
                .existsByDialogIdAndUserId(dialogId, userId)
                .doOnError(error ->
                        log.error(
                                "canAccess ERROR: dialogId={}, userId={}",
                                dialogId,
                                userId,
                                error
                        )
                );
    }
}