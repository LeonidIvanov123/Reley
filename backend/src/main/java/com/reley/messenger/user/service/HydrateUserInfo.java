package com.reley.messenger.user.service;

import com.reley.messenger.user.dto.CurrentUserInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class HydrateUserInfo implements HydrateUserInfoApi {

    public Mono<CurrentUserInfo> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .filter(authentication -> authentication != null)
                .map(Authentication::getPrincipal)
                .cast(Jwt.class)
                .map(jwt -> new CurrentUserInfo(
                        Long.valueOf(jwt.getSubject()),
                        jwt.getClaimAsString("email")
                ));
    }

    @Override
    public Mono<Long> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .filter(authentication -> authentication != null)
                .map(Authentication::getPrincipal)
                .cast(Jwt.class)
                .map(jwt -> Long.valueOf(jwt.getSubject()));
    }
}
