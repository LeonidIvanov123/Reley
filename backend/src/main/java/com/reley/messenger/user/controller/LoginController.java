package com.reley.messenger.user.controller;

import com.reley.messenger.configuration.AppVersion;
import com.reley.messenger.configuration.SecurityUtils;
import com.reley.messenger.user.dto.LoginRequest;
import com.reley.messenger.user.service.AuthenticationService;
import com.reley.messenger.user.service.RefreshSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import static com.reley.messenger.configuration.SecurityUtils.ACCESS_TOKEN_COOKIE;
import static com.reley.messenger.configuration.SecurityUtils.REFRESH_TOKEN_COOKIE;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LoginController {

    private final AuthenticationService authenticationService;
    private final RefreshSessionService refreshSessionService;
    private final AppVersion appVersion;


    @GetMapping("/login")
    public Mono<String> login(Model model) {
        model.addAttribute("appVersion", appVersion.getVersion());

        return Mono.just("login");
    }

    @PostMapping("/login")
    public Mono<String> login(
            @ModelAttribute LoginRequest loginRequest,
            ServerWebExchange exchange) {

        return authenticationService
                .authenticate(loginRequest.email(), loginRequest.password())
                .doOnNext(authentication -> {
                    exchange.getResponse().addCookie(
                            SecurityUtils.createAccessTokenCookie(authentication.accessToken())
                    );
                    exchange.getResponse().addCookie(
                            SecurityUtils.createRefreshTokenCookie(authentication.refreshToken())
                    );
                })
                .thenReturn("redirect:/");
    }

    @PostMapping("/auth/refresh")
    public Mono<Void> refresh(ServerWebExchange exchange) {
        String refreshToken = Objects.requireNonNull(exchange.getRequest()
                        .getCookies()
                        .getFirst(REFRESH_TOKEN_COOKIE)).getValue();

        return authenticationService.refresh(refreshToken)
                .doOnNext(authentication -> {
                    exchange.getResponse().addCookie(
                            SecurityUtils.createAccessTokenCookie(authentication.accessToken())
                    );

                    exchange.getResponse().addCookie(
                            SecurityUtils.createRefreshTokenCookie(authentication.refreshToken())
                    );
                })
                .then(Mono.fromRunnable(() ->
                        exchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT)
                ));
    }

    @PostMapping("/logout")
    public Mono<Void> logout(ServerWebExchange exchange) {
        String refreshToken = Optional.ofNullable(
                exchange.getRequest()
                        .getCookies()
                        .getFirst(REFRESH_TOKEN_COOKIE)
        ).map(HttpCookie::getValue).orElse(null);

        return refreshSessionService.revokeByRefreshToken(refreshToken)
                .doOnSuccess(unused ->
                        log.info("User session revoked during logout")
                )
                .onErrorResume(error -> {
                    log.warn("Failed to revoke session during logout", error);
                    return Mono.empty();
                })
                .then(Mono.fromRunnable(() -> {
                    exchange.getResponse().addCookie(
                            SecurityUtils.deleteCookie(ACCESS_TOKEN_COOKIE)
                    );
                    exchange.getResponse().addCookie(
                            SecurityUtils.deleteCookie(REFRESH_TOKEN_COOKIE)
                    );

                    exchange.getResponse().setStatusCode(HttpStatus.SEE_OTHER);
                    exchange.getResponse().getHeaders()
                            .setLocation(URI.create("/login"));
                }));
    }
}
