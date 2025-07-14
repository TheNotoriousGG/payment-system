package org.example.individualsapi.util;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimAccessor;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

public class AuthContextUtil {

    public static Mono<String> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(auth -> (Jwt) auth.getPrincipal())
                .map(JwtClaimAccessor::getSubject);
    }
}
