package org.example.individualsapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.individualsapi.mapper.UserMapper;
import org.example.individualsapi.model.dto.TokenResponse;
import org.example.individualsapi.model.dto.UserInfoResponse;
import org.example.individualsapi.model.dto.UserRegistrationRequest;
import org.example.individualsapi.util.AuthContextUtil;
import org.example.individualsapi.util.RequestValidator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static org.example.individualsapi.util.StrUtils.usernameFromEmail;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final TokenService tokenService;
    private final UserMapper userMapper;
    private final KeycloakClient keycloakClient;

    public Mono<TokenResponse> userRegistration(Mono<UserRegistrationRequest> userRegistrationRequest) {
        return userRegistrationRequest
                .flatMap(RequestValidator::validateUserRegistrationRequest)
                .flatMap(request ->
                        tokenService.getServiceToken()
                                .flatMap(
                                        serviceToken ->
                                                keycloakClient.addNewKeycloakUser(request.getEmail(), request.getPassword(), serviceToken)
                                ).then(tokenService.getUserToken(
                                        usernameFromEmail(request.getEmail()), request.getPassword())
                                ));
    }

    public Mono<TokenResponse> userLogin(String email, String password) {
        return tokenService.getUserToken(usernameFromEmail(email), password);
    }

    public Mono<TokenResponse> refreshToken(String refreshToken) {
        return tokenService.refreshToken(refreshToken);
    }

    public Mono<UserInfoResponse> getAuthUserInfo() {
        log.info("Get auth user info");

        return tokenService.getServiceToken()
                .doOnSuccess(token -> log.debug("Service token obtained successfully"))
                .doOnError(error -> log.error("Failed to get service token: {}", error.getMessage(), error))
                .flatMap(serviceToken ->
                        AuthContextUtil.getCurrentUserId()
                                .doOnSuccess(userId -> log.debug("Current user ID extracted: {}", userId))
                                .doOnError(error -> log.error("Failed to extract current user ID: {}", error.getMessage(), error))
                                .flatMap(userId ->
                                        Mono.zip(
                                                keycloakClient.getUserInfo(userId, serviceToken)
                                                        .doOnSuccess(userInfo -> log.debug("User info retrieved successfully for user: {}", userId))
                                                        .doOnError(error -> log.error("Failed to get user info for user {}: {}", userId, error.getMessage(), error)),
                                                keycloakClient.getUserRoles(userId, serviceToken)
                                                        .doOnSuccess(roles -> log.debug("User roles retrieved successfully for user: {}", userId))
                                                        .doOnError(error -> log.error("Failed to get user roles for user {}: {}", userId, error.getMessage(), error))
                                        )
                                        .doOnSuccess(tuple -> log.debug("Both user info and roles retrieved successfully for user: {}", userId))
                                        .doOnError(error -> log.error("Failed to retrieve user info or roles for user {}: {}", userId, error.getMessage(), error))
                                )
                                .map(tuple -> {
                                    UserInfoResponse userInfoResponse = userMapper.toUserInfoResponse(tuple.getT1());
                                    userInfoResponse.roles(
                                            keycloakClient.extractClientRoles(tuple.getT2())
                                    );

                                    return userInfoResponse;
                                })
                ).doOnSuccess(_ -> log.info("Auth user info received"))
                .doOnError(error -> log.error("Error in try to get auth user info: {}", error.getMessage(), error));
    }
}
