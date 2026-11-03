package com.reley.messenger.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

import static com.reley.messenger.configuration.SecurityUtils.ACCESS_TOKEN_COOKIE;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserRedirectFilter implements WebFilter {

    private final ReactiveJwtDecoder jwtDecoder;

    private static final Set<String> REDIRECT_PATHS = Set.of(
            "/login",
            "/register"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String path = exchange.getRequest().getPath().value();

        if (!REDIRECT_PATHS.contains(path)) {
            return chain.filter(exchange);
        }

        String token = Optional.ofNullable(
                        exchange.getRequest().getCookies()
                                .getFirst(ACCESS_TOKEN_COOKIE)
                )
                .map(HttpCookie::getValue)
                .orElse(null);

        if (token == null) {
            return chain.filter(exchange);
        }

        return jwtDecoder.decode(token)
                .flatMap(jwt -> {
                    exchange.getResponse().setStatusCode(HttpStatus.SEE_OTHER);
                    exchange.getResponse().getHeaders()
                            .setLocation(URI.create("/"));

                    return exchange.getResponse().setComplete();
                })
                .onErrorResume(JwtException.class, error ->
                        chain.filter(exchange)
                );
    }
}