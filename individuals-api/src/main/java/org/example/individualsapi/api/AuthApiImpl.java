package org.example.individualsapi.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.individualsapi.model.dto.*;
import org.example.individualsapi.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @Override
    public Mono<ResponseEntity<TokenResponse>> authLoginPost(Mono<UserLoginRequest> userLoginRequest, ServerWebExchange exchange) {
        return userLoginRequest
                .flatMap( request ->
                        userService.userLogin(request.getEmail(), request.getPassword())
                ).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<UserInfoResponse>> authMeGet(ServerWebExchange exchange) {
        return userService.getAuthUserInfo()
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TokenResponse>> authRefreshTokenPost(Mono<TokenRefreshRequest> tokenRefreshRequest, ServerWebExchange exchange) {
        return tokenRefreshRequest
                .flatMap( request ->
                        userService.refreshToken(request.getRefreshToken())
                ).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TokenResponse>> authRegistrationPost(@RequestBody Mono<UserRegistrationRequest> userRegistrationRequest, ServerWebExchange exchange) {
        return userRegistrationRequest
                .flatMap(request ->
                        userService.userRegistration(Mono.just(request))
                ).map(tokenResponse -> ResponseEntity.status(HttpStatus.CREATED).body(tokenResponse));
    }
}
