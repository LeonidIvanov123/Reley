package com.reley.messenger.configuration;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class CookieBearerTokenAuthenticationConverter extends ServerBearerTokenAuthenticationConverter {

    private static final String ACCESS_TOKEN_COOKIE = "RELEY_ACCESS_TOKEN";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        String authorization = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authorization != null && authorization.startsWith("Bearer ")) {
            return Mono.just(new BearerTokenAuthenticationToken(
                    authorization.substring(7)
            ));
        }

        String token = Optional.ofNullable(
                exchange.getRequest()
                        .getCookies()
                        .getFirst(ACCESS_TOKEN_COOKIE)
        ).map(HttpCookie::getValue).orElse(null);

        if (token == null) {
            return Mono.empty();
        }

        return Mono.just(new BearerTokenAuthenticationToken(token));
    }

}
