package com.reley.messenger.user.service;

import com.reley.messenger.user.dto.CurrentUserInfo;
import reactor.core.publisher.Mono;

public interface HydrateUserInfoApi {

    Mono<CurrentUserInfo> getCurrentUser();

    Mono<Long> getCurrentUserId();
}
