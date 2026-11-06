package com.reley.messenger.dialog.service;

import reactor.core.publisher.Mono;

public interface DialogReadStateService {

    Mono<Void> markAsRead(Long dialogId, Long userId);

    Mono<Long> getLastReadMessageId(Long dialogId, Long userId);

    Mono<Long> getUnreadCount(Long dialogId, Long userId);

}
