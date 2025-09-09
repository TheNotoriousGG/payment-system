package org.example.individualsapi.service;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.individualsapi.config.KeycloakProperties;
import org.example.individualsapi.model.*;
import org.example.individualsapi.model.dto.TokenResponse;
import org.example.individualsapi.util.RequestBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;

import static org.example.individualsapi.util.ErrorHandlingUtil.keycloakHttpErrorMapper;
import static org.example.individualsapi.util.RequestBuilder.buildGetTokenRequestFormData;
import static org.example.individualsapi.util.RequestBuilder.buildRefreshTokenRequestFormData;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakClient {

    private final KeycloakProperties keycloakProperties;
    private final WebClient webClient;

    public Mono<Void> addNewKeycloakUser(String email, String password, String serviceToken) {
        log.info("Adding new user {}", email);

        KeycloakUserRequest requestBody = RequestBuilder.buildKeycloakUserRequest(email, password);
        log.info("Request body {}", requestBody);

        return webClient.post()
                .uri(keycloakProperties.getUsersEndpoint())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .onStatus(HttpStatusCode::isError, keycloakHttpErrorMapper())
                .bodyToMono(Void.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof WebClientRequestException))
                .doOnSuccess(_ -> log.info("User {} successfully added", email))
                .doOnError(throwable -> log.error("Error in try to add new user: {} -> {}", email, throwable.getMessage()));
    }

    public Mono<KeycloakRoleMappingResponse> getUserRoles(String userId, String serviceToken) {
        log.info("Get user roles");

        return webClient.get()
                .uri(keycloakProperties.getUserRolesEndpoint(userId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, keycloakHttpErrorMapper())
                .bodyToMono(KeycloakRoleMappingResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof WebClientRequestException))
                .doOnSuccess(_ -> log.info("User roles successfully received from keycloak userId = {}", userId))
                .doOnError(throwable -> log.error("Error in try to get user roles from keycloak userId = {}, error = {}", userId, throwable.getMessage()));
    }

    public Mono<KeycloakUserResponse> getUserInfo(String userId, String serviceToken) {
        log.info("Get user info for user id {}", userId);

        return webClient.get()
                .uri(keycloakProperties.getUserEndpoint(userId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer "+ serviceToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, keycloakHttpErrorMapper())
                .bodyToMono(KeycloakUserResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof WebClientRequestException))
                .doOnSuccess(_ -> log.info("User info successfully received from keycloak userId =  {}", userId))
                .doOnError(throwable -> log.error("Error in try to get user from keycloak userId = {}, error = {}", userId, throwable.getMessage()));
    }

    public List<String> extractClientRoles(KeycloakRoleMappingResponse roleMappings) {
        if (roleMappings == null) {
            return Collections.emptyList();
        }

        Map<String, KeycloakClientMapping> clientMappings = roleMappings.getClientMappings();
        if (clientMappings == null) {
            return Collections.emptyList();
        }

        KeycloakClientMapping clientMapping = clientMappings.get(keycloakProperties.getClientId());
        return clientMapping != null && clientMapping.getMappings() != null
                ? clientMapping.getMappings().stream()
                .filter(Objects::nonNull)
                .map(KeycloakRoleMapping::getName)
                .filter(Objects::nonNull)
                .toList()
                : Collections.emptyList();
    }

    public Mono<TokenResponse> authenticateUser(String username, String password) {

        var formData = buildGetTokenRequestFormData(
                username,
                password,
                keycloakProperties.getClientId(),
                keycloakProperties.getClientSecret()
        );

        log.debug("Request FormData: {}", formData);

        return webClient.post()
                .uri(keycloakProperties.getTokenEndpoint())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatusCode::isError, keycloakHttpErrorMapper())
                .bodyToMono(TokenResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof WebClientRequestException))
                .doOnSuccess(token -> {
                    log.info("Token for user {} successfully received", username);
                    log.debug("Token response: access_token length={}, refresh_token length={}, expires_in={}",
                            token.getAccessToken() != null ? token.getAccessToken().length() : 0,
                            token.getRefreshToken() != null ? token.getRefreshToken().length() : 0,
                            token.getExpiresIn());
                })
                .doOnError(throwable -> log.error("Error getting token for user: {} -> {}", username, throwable.getMessage()));
    }

    public Mono<TokenResponse> refreshToken(@NotNull String refreshToken) {

        var formData = buildRefreshTokenRequestFormData(
                refreshToken,
                keycloakProperties.getClientId(),
                keycloakProperties.getClientSecret()
        );

        log.debug("Refresh FormData: {}", formData);

        return webClient.post()
                .uri(keycloakProperties.getTokenEndpoint())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatusCode::isError, keycloakHttpErrorMapper())
                .bodyToMono(TokenResponse.class)
                .doOnSuccess(token -> {
                    log.info("Token successfully refreshed");
                    log.debug("Refresh response: access_token length={}, refresh_token length={}, expires_in={}",
                            token.getAccessToken() != null ? token.getAccessToken().length() : 0,
                            token.getRefreshToken() != null ? token.getRefreshToken().length() : 0,
                            token.getExpiresIn());
                })
                .doOnError(throwable -> log.error("Error refreshing token: {}", throwable.getMessage()));
    }

    public Mono<TokenResponse> fetchServiceToken() {
        return authenticateUser(keycloakProperties.getAdminUsername(), keycloakProperties.getAdminPassword());
    }
}
