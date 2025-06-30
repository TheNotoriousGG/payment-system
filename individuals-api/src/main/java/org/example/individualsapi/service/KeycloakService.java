package org.example.individualsapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.individualsapi.exception.KeycloakApiException;
import org.example.individualsapi.model.KeycloakError;
import org.example.individualsapi.model.KeycloakUserCredentials;
import org.example.individualsapi.model.KeycloakUserRequest;
import org.example.individualsapi.model.dto.ErrorResponse;
import org.example.individualsapi.model.dto.TokenResponse;
import org.example.individualsapi.model.dto.UserInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakService {

    private final WebClient webClient;

    @Value("${keycloak.client_id}")
    private String clientId;

    @Value("${keycloak.client_secret}")
    private String clientSecret;

    @Value("${keycloak.realm_name}")
    private String realmName;

    @Value("${keycloak.admin_username}")
    private String adminUsername;

    @Value("${keycloak.admin_password}")
    private String adminPassword;

    public Mono<UserInfoResponse> getUserInfo(@NotNull String userId, String adminToken) {
        log.info("Get user info for user id {}", userId);

        return webClient.get()
                .uri(String.format("/admin/realms/%s/users/%s",realmName, userId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer "+ adminToken)
                .retrieve()
                .onStatus(httpStatusCode ->
                                httpStatusCode.is4xxClientError() ||
                                        httpStatusCode.is5xxServerError(),
                        keycloakErrorHandler()
                ).bodyToMono(UserInfoResponse.class)
                .doOnSuccess(_ -> log.info("User info successfully received userId =  {}", userId))
                .doOnError(throwable -> log.info("Error in try to get user info userId = {}, error = {}", userId, throwable.getMessage()));
    }

    public Mono<TokenResponse> getUserToken(String username, String password) {
        log.info("Getting user token for {}", username);

        MultiValueMap<String, String> formData = createTokenRequestBody(username, password);

        return webClient.post()
                .uri(String.format("/realms/%s/protocol/openid-connect/token",realmName))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(httpStatusCode ->
                        httpStatusCode.is4xxClientError() ||
                                httpStatusCode.is5xxServerError(),
                        keycloakErrorHandler()
                )
                .bodyToMono(TokenResponse.class)
                .doOnSuccess(_ -> log.info("Token for user {} successfully received", username))
                .doOnError(throwable -> log.error("Error in try to get token for user: {} -> {}",username, throwable.getMessage()));
    }

    public Mono<TokenResponse> refreshToken(@NotNull String refreshToken) {

        MultiValueMap<String,String> formData = createRefreshTokenRequest(refreshToken);

        return webClient.post()
                .uri(String.format("/realms/%s/protocol/openid-connect/token",realmName))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(httpStatusCode ->
                                httpStatusCode.is4xxClientError() ||
                                        httpStatusCode.is5xxServerError(),
                        keycloakErrorHandler()
                ).bodyToMono(TokenResponse.class)
                .doOnSuccess(_ -> log.info("Token successfully refreshed"))
                .doOnError(throwable -> log.error("Error in try to refresh token -> {}", throwable.getMessage()));


    }

    public Mono<Void> addNewUser(String email, String password, String username, String adminToken) {

        KeycloakUserRequest requestBody = KeycloakUserRequest.builder()
                .email(email)
                .username(username)
                .enabled(true)
                .emailVerified(true)
                .firstName(username)
                .lastName(username)
                .credentials(
                        List.of(
                                KeycloakUserCredentials
                                        .builder()
                                        .temporary(false)
                                        .type("password")
                                        .value(password)
                                        .build()
                        )
                )
                .build();

        log.info("Adding new user {}", email);
        log.debug("Request body {}", requestBody);

        return webClient.post()
                .uri(String.format("admin/realms/%s/users",realmName))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .onStatus(httpStatusCode ->
                                httpStatusCode.is4xxClientError() ||
                                httpStatusCode.is5xxServerError(),
                        keycloakErrorHandler())
                .bodyToMono(Void.class)
                .doOnSuccess(_ -> log.info("User {} successfully added", email))
                .doOnError(throwable -> log.error("Error in try to add new user: {} -> {}", email, throwable.getMessage()));
    }

    public Mono<TokenResponse> getAdminToken() {
        return getUserToken(adminUsername, adminPassword);
    }

    private MultiValueMap<String, String> createTokenRequestBody(String username, String password) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("username", username);
        formData.add("password", password);
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        return formData;
    }

    private MultiValueMap<String, String> createRefreshTokenRequest(String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("refresh_token", refreshToken);
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        return formData;
    }

    private Function<ClientResponse, Mono<? extends Throwable>> keycloakErrorHandler() {
        return clientResponse ->
                clientResponse.bodyToMono(KeycloakError.class)
                        .flatMap(error -> {
                            String message = String.format("%s: %s", error.getError(), error.getErrorDescription());

                            ErrorResponse errorResponse = new ErrorResponse();
                            errorResponse.setStatus(clientResponse.statusCode().value());
                            errorResponse.setError(message);

                            return Mono.error(new KeycloakApiException(errorResponse, message));
                        });
    }

}
