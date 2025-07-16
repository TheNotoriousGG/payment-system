package org.example.individualsapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.individualsapi.config.KeycloakProperties;
import org.example.individualsapi.exception.RequestValidationException;
import org.example.individualsapi.mapper.UserMapper;
import org.example.individualsapi.model.KeycloakRoleMapping;
import org.example.individualsapi.model.KeycloakRoleMappingResponse;
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

import java.util.List;
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
                                            getUserInfo(userId, serviceToken),
                                            getUserRoles(userId, serviceToken)
                                         )
                                )
                                .map(tuple -> {
                                    UserInfoResponse userInfoResponse = userMapper.toUserInfoResponse(tuple.getT1());
                                    userInfoResponse.roles(
                                            extractClientRoles(tuple.getT2())
                                    );

                                    return userInfoResponse;
                                })
                ).doOnSuccess(_ -> log.info("Auth user info received"))
                .doOnError(_ -> log.error("Error in try to get auth user info"));
    }

    public Mono<KeycloakRoleMappingResponse> getUserRoles(String userId, String adminToken) {
        log.info("Get user roles");

        return webClient.get()
                .uri(keycloakProperties.getUserRolesEndpoint(userId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, keycloakErrorHandler())
                .bodyToMono(KeycloakRoleMappingResponse.class)
                .doOnSuccess(_ -> log.info("User roles successfully received from keycloak userId = {}", userId))
                .doOnError(throwable -> log.error("Error in try to get user roles from keycloak userId = {}, error = {}", userId, throwable.getMessage()));
    }

    private Mono<Void> addNewKeycloakUser(String email, String password) {
        log.info("Adding new user {}", email);

        KeycloakUserRequest requestBody = RequestBuilder.buildKeycloakUserRequest(email, password);
        log.info("Request body {}", requestBody);

        return tokenService.getServiceToken()
            .flatMap(token ->
                webClient.post()
                    .uri(keycloakProperties.getUserInfoEndpoint())
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

    private Mono<KeycloakUserResponse> getUserInfo(String userId, String adminToken) {
        log.info("Get user info for user id {}", userId);

        return webClient.get()
                .uri(keycloakProperties.getUserEndpoint(userId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer "+ adminToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, keycloakErrorHandler())
                .bodyToMono(KeycloakUserResponse.class)
                .doOnSuccess(_ -> log.info("User info successfully received from keycloak userId =  {}", userId))
                .doOnError(throwable -> log.error("Error in try to get user from keycloak userId = {}, error = {}", userId, throwable.getMessage()));
    }

    private Mono<UserRegistrationRequest> validateUserRegistrationRequest(UserRegistrationRequest request) {
        if (!Objects.equals(request.getPassword(), request.getConfirmPassword())) {
            return Mono.error(new RequestValidationException("Passwords do not match"));
        }
        return Mono.just(request);
    }

    private List<String> extractClientRoles(KeycloakRoleMappingResponse roleMappings) {
        return roleMappings.getClientMappings().get(keycloakProperties.getClientId())
                .getMappings().stream()
                .map(KeycloakRoleMapping::getName)
                .toList();
    }
}
