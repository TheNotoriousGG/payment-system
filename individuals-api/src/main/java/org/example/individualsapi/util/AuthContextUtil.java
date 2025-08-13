package org.example.individualsapi.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimAccessor;
import reactor.core.publisher.Mono;

@Slf4j
public class AuthContextUtil {

    public static Mono<String> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .doOnError(error -> log.error("Failed to get security context: {}", error.getMessage(), error))
                .map(SecurityContext::getAuthentication)
                .doOnError(error -> log.error("Failed to get authentication: {}", error.getMessage(), error))
                .map(auth -> (Jwt) auth.getPrincipal())
                .doOnError(error -> log.error("Failed to cast to JWT: {}", error.getMessage(), error))
                .map(JwtClaimAccessor::getSubject)
                .doOnSuccess(userId -> log.debug("Successfully extracted user ID: {}", userId))
                .doOnError(error -> log.error("Failed to extract subject from JWT: {}", error.getMessage(), error));
    }
}
