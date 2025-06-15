package org.example.individualsapi.api;

import org.example.individualsapi.model.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1")
public class AuthApiImpl implements AuthApi {


    @Override
    public Mono<ResponseEntity<TokenResponse>> authLoginPost(Mono<UserLoginRequest> userLoginRequest, ServerWebExchange exchange) {
        return Mono.empty();
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
    public Mono<ResponseEntity<TokenResponse>> authRegistrationPost(Mono<UserRegistrationRequest> userRegistrationRequest, ServerWebExchange exchange) {
        return null;
    }
}
