package com.reley.messenger.configuration;

import com.reley.messenger.exception.InvalidRefreshTokenException;
import com.reley.messenger.user.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TokenRefreshWebFilter implements WebFilter {

    private static final String ACCESS_TOKEN_COOKIE = "RELEY_ACCESS_TOKEN";
    private static final String REFRESH_TOKEN_COOKIE = "RELEY_REFRESH_TOKEN";

    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/login",
            "/register",
            "/auth/refresh",
            "/logout"
    );

    private final ReactiveJwtDecoder jwtDecoder;
    private final AuthenticationService authenticationService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (EXCLUDED_PATHS.contains(path) || path.startsWith("/css/")
                || path.startsWith("/js/") || path.startsWith("/images/")) {
            return chain.filter(exchange);
        }

        String accessToken = getCookie(exchange, ACCESS_TOKEN_COOKIE);
        String refreshToken = getCookie(exchange, REFRESH_TOKEN_COOKIE);

        if (refreshToken == null) {
            return chain.filter(exchange);
        }

        if (accessToken == null) {
            return refreshAndContinue(exchange, chain, refreshToken);
        }

        return jwtDecoder.decode(accessToken)
                .flatMap(jwt -> chain.filter(exchange))
                .onErrorResume(JwtException.class,
                        error -> refreshAndContinue(exchange, chain, refreshToken));
    }

    private Mono<Void> refreshAndContinue(ServerWebExchange exchange, WebFilterChain chain, String refreshToken) {
        return authenticationService.refresh(refreshToken)
                .flatMap(authentication -> {
                    exchange.getResponse().addCookie(
                            createAccessTokenCookie(authentication.accessToken())
                    );

                    exchange.getResponse().addCookie(
                            createRefreshTokenCookie(authentication.refreshToken())
                    );

                    ServerWebExchange updatedExchange = exchange.mutate()
                            .request(request -> request.headers(headers ->
                                    headers.setBearerAuth(authentication.accessToken())
                            ))
                            .build();

                    return chain.filter(updatedExchange);
                })
                .onErrorResume(InvalidRefreshTokenException.class,
                        error -> chain.filter(exchange));
    }

    private String getCookie(ServerWebExchange exchange, String name) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(name);
        return cookie != null ? cookie.getValue() : null;
    }

    private ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMinutes(5))
                .build();
    }

    private ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(5))
                .build();
    }
}