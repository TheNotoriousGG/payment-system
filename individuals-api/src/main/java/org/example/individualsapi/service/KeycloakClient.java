package org.example.individualsapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.individualsapi.config.KeycloakProperties;
import org.example.individualsapi.model.KeycloakRoleMapping;
import org.example.individualsapi.model.KeycloakRoleMappingResponse;
import org.example.individualsapi.model.KeycloakUserRequest;
import org.example.individualsapi.model.KeycloakUserResponse;
import org.example.individualsapi.util.RequestBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.example.individualsapi.util.ErrorHandlingUtil.keycloakHttpErrorMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakClient {

    private final KeycloakProperties keycloakProperties;
    private final WebClient webClient;


    public Mono<Void> addNewKeycloakUser(String email, String password, String serviceToken) {
        log.info("Adding new user {}", email);

        KeycloakUserRequest requestBody = RequestBuilder.buildKeycloakUserRequest(email, password);
        log.info("Request body {}", requestBody);

        return webClient.post()
                .uri(keycloakProperties.getUserInfoEndpoint())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .onStatus(HttpStatusCode::isError, keycloakHttpErrorMapper())
                .bodyToMono(Void.class)
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
                .doOnSuccess(_ -> log.info("User info successfully received from keycloak userId =  {}", userId))
                .doOnError(throwable -> log.error("Error in try to get user from keycloak userId = {}, error = {}", userId, throwable.getMessage()));
    }

    public List<String> extractClientRoles(KeycloakRoleMappingResponse roleMappings) {
        return roleMappings.getClientMappings().get(keycloakProperties.getClientId())
                .getMappings().stream()
                .map(KeycloakRoleMapping::getName)
                .toList();
    }
}
