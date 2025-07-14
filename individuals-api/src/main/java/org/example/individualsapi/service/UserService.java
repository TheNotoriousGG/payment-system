package org.example.individualsapi.service;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.individualsapi.config.KeycloakProperties;
import org.example.individualsapi.exception.RequestValidationException;
import org.example.individualsapi.mapper.UserMapper;
import org.example.individualsapi.model.KeycloakUserRequest;
import org.example.individualsapi.model.KeycloakUserResponse;
import org.example.individualsapi.model.dto.TokenResponse;
import org.example.individualsapi.model.dto.UserInfoResponse;
import org.example.individualsapi.model.dto.UserRegistrationRequest;
import org.example.individualsapi.util.AuthContextUtil;
import org.example.individualsapi.util.RequestBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static org.example.individualsapi.util.ErrorHandlingUtil.keycloakErrorHandler;
import static org.example.individualsapi.util.StrUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final WebClient webClient;
    private final TokenService tokenService;
    private final UserMapper userMapper;
    private final KeycloakProperties keycloakProperties;

    public Mono<TokenResponse> userRegistration(Mono<UserRegistrationRequest> userRegistrationRequest) {

        return userRegistrationRequest
                .flatMap(this::validateUserRegistrationRequest)
                .flatMap(request ->
                        addNewKeycloakUser(request.getEmail(), request.getPassword())
                                .then(tokenService.getUserToken(
                                        usernameFromEmail(request.getEmail()),request.getPassword()
                                )
                        ));
    }

    private Mono<Void> addNewKeycloakUser(String email, String password) {
        log.info("Adding new user {}", email);

        KeycloakUserRequest requestBody = RequestBuilder.buildKeycloakUserRequest(email, password);
        log.info("Request body {}", requestBody);

        return tokenService.getServiceToken()
            .doOnNext(token -> log.debug("Using service token: {}...", token.substring(0, Math.min(20, token.length()))))
            .flatMap(token -> 
                webClient.post()
                    .uri(keycloakProperties.getAdminUsersUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, keycloakErrorHandler())
                    .bodyToMono(Void.class)
            )
            .doOnSuccess(_ -> log.info("User {} successfully added", email))
            .doOnError(throwable -> log.error("Error in try to add new user: {} -> {}", email, throwable.getMessage()));
    }

    public Mono<KeycloakUserResponse> getUserInfo(String userId, String adminToken) {
        log.info("Get user info for user id {}", userId);

        return webClient.get()
                .uri(keycloakProperties.getAdminUsersUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer "+ adminToken)
                .retrieve()
                .onStatus(httpStatusCode ->
                                httpStatusCode.is4xxClientError() ||
                                        httpStatusCode.is5xxServerError(),
                        keycloakErrorHandler()
                ).bodyToMono(KeycloakUserResponse.class)
                .doOnSuccess(_ -> log.info("User info successfully received userId =  {}", userId))
                .doOnError(throwable -> log.info("Error in try to get user info userId = {}, error = {}", userId, throwable.getMessage()));
    }

    public Mono<TokenResponse> userLogin(String email, String password) {
        return tokenService.getUserToken(usernameFromEmail(email), password);
    }

    public Mono<TokenResponse> refreshToken(String refreshToken) {
        return tokenService.refreshToken(refreshToken);
    }

    private Mono<UserRegistrationRequest> validateUserRegistrationRequest(UserRegistrationRequest request) {
        if (!Objects.equals(request.getPassword(), request.getConfirmPassword())) {
            return Mono.error(new RequestValidationException("Passwords do not match"));
        }
        return Mono.just(request);
    }
}
