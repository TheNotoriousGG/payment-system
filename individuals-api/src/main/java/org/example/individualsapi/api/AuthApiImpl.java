package org.example.individualsapi.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.individualsapi.model.dto.*;
import org.example.individualsapi.service.KeycloakService;
import org.example.individualsapi.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AuthApiImpl implements AuthApi {

    private final UserService userService;
    private final Validator validator;
    private final KeycloakService keycloakService;


    @Override
    public Mono<ResponseEntity<TokenResponse>> authLoginPost(Mono<UserLoginRequest> userLoginRequest, ServerWebExchange exchange) {
        return null;
    }

    @Override
    public Mono<ResponseEntity<UserInfoResponse>> authMeGet(ServerWebExchange exchange) {
        return null;
    }

    @Override
    public Mono<ResponseEntity<TokenResponse>> authRefreshTokenPost(Mono<TokenRefreshRequest> tokenRefreshRequest, ServerWebExchange exchange) {
        return null;
    }

    @Override
    public Mono<ResponseEntity<TokenResponse>> authRegistrationPost(@RequestBody Mono<UserRegistrationRequest> userRegistrationRequest, ServerWebExchange exchange) {

        return userRegistrationRequest
                .flatMap(request ->
                        userService.userRegistration(Mono.just(request))
                )
                .map(tokenResponse -> ResponseEntity.status(HttpStatus.CREATED).body(tokenResponse));

    }
}
