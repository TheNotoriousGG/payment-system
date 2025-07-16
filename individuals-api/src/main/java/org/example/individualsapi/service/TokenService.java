package org.example.individualsapi.service;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.individualsapi.config.KeycloakProperties;
import org.example.individualsapi.model.dto.TokenResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.example.individualsapi.util.ErrorHandlingUtil.keycloakErrorHandler;
import static org.example.individualsapi.util.RequestBuilder.buildGetTokenRequestFormData;
import static org.example.individualsapi.util.RequestBuilder.buildRefreshTokenRequestFormData;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {

    private final WebClient webClient;
    private final KeycloakProperties keycloakProperties;
    private final AtomicReference<TokenResponse> serviceToken = new AtomicReference<>();
    private final AtomicReference<LocalDateTime> serviceAccessTokenExpireMoment = new AtomicReference<>();
    private final AtomicReference<LocalDateTime> serviceRefreshTokenExpireMoment = new AtomicReference<>();

    @PostConstruct
    public void init() {
        log.info("Initializing service token");
        fetchServiceToken()
            .doOnNext(this::manageServiceTokenState)
            .subscribe(
                    _ -> log.info("Service token initialized successfully"),
                error -> log.error("Failed to initialize service token: {}", error.getMessage())
            );
    }

    private Mono<TokenResponse> fetchServiceToken() {
        return getUserToken(keycloakProperties.getAdminUsername(), keycloakProperties.getAdminPassword());
    }

    private void manageServiceTokenState(TokenResponse tokenResponse) {
        serviceToken.set(tokenResponse);
        serviceAccessTokenExpireMoment.set(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn() - 10));
        serviceRefreshTokenExpireMoment.set(LocalDateTime.now().plusSeconds(tokenResponse.getRefreshExpiresIn() - 10));
        log.info("Service token state updated");
    }

    public Mono<String> getServiceToken() {
        LocalDateTime accessExpiry = serviceAccessTokenExpireMoment.get();
        LocalDateTime refreshExpiry = serviceRefreshTokenExpireMoment.get();
        TokenResponse currentToken = serviceToken.get();
        
        if (currentToken == null || accessExpiry == null || refreshExpiry == null) {
            log.info("Token not initialized, fetching new one");
            return fetchServiceToken()
                .doOnNext(this::manageServiceTokenState)
                .map(TokenResponse::getAccessToken);
        }

        boolean isAccessTokenExpired = accessExpiry.isBefore(LocalDateTime.now());
        boolean isRefreshTokenExpired = refreshExpiry.isBefore(LocalDateTime.now());

        if (!isAccessTokenExpired) {
            return Mono.just(currentToken.getAccessToken());
        } else if (!isRefreshTokenExpired) {
            log.info("Access token expired, refreshing");
            return refreshServiceToken()
                .doOnNext(this::manageServiceTokenState)
                .map(TokenResponse::getAccessToken);
        } else {
            log.info("Both tokens expired, fetching new");
            return fetchServiceToken()
                .doOnNext(this::manageServiceTokenState)
                .map(TokenResponse::getAccessToken);
        }
    }

    private Mono<TokenResponse> refreshServiceToken() {
        TokenResponse currentToken = serviceToken.get();
        if (currentToken == null || currentToken.getRefreshToken() == null) {
            return Mono.error(new IllegalStateException("No refresh token available"));
        }
        return refreshToken(currentToken.getRefreshToken());
    }

    public Mono<TokenResponse> getUserToken(String username, String password) {
        log.info("Getting user token for {}", username);

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
                .onStatus(HttpStatusCode::isError, keycloakErrorHandler())
                .bodyToMono(TokenResponse.class)
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
        log.debug("Refreshing token");

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
                .onStatus(HttpStatusCode::isError, keycloakErrorHandler())
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
}
