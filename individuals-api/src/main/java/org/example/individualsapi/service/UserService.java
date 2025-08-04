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
                .flatMap(serviceToken ->
                        AuthContextUtil.getCurrentUserId()
                                .flatMap(userId ->
                                        Mono.zip(
                                                keycloakClient.getUserInfo(userId, serviceToken),
                                                keycloakClient.getUserRoles(userId, serviceToken)
                                        )
                                )
                                .map(tuple -> {
                                    UserInfoResponse userInfoResponse = userMapper.toUserInfoResponse(tuple.getT1());
                                    userInfoResponse.roles(
                                            keycloakClient.extractClientRoles(tuple.getT2())
                                    );

                                    return userInfoResponse;
                                })
                ).doOnSuccess(_ -> log.info("Auth user info received"))
                .doOnError(_ -> log.error("Error in try to get auth user info"));
    }
}
