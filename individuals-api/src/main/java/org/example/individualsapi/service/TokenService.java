package org.example.individualsapi.service;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.individualsapi.model.dto.TokenResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {

    private final AtomicReference<TokenResponse> serviceToken = new AtomicReference<>();
    private final AtomicReference<LocalDateTime> serviceAccessTokenExpireMoment = new AtomicReference<>();
    private final AtomicReference<LocalDateTime> serviceRefreshTokenExpireMoment = new AtomicReference<>();
    private final KeycloakClient keycloakClient;

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

    public Mono<TokenResponse> getUserToken(String username, String password) {
        log.info("Getting user token for {}", username);

        return keycloakClient.authenticateUser(username, password);
    }

    public Mono<TokenResponse> refreshToken(@NotNull String refreshToken) {
        log.debug("Refreshing token");

        return keycloakClient.refreshToken(refreshToken);
    }

    private Mono<TokenResponse> fetchServiceToken() {
        log.info("Fetching service token");

        return keycloakClient.fetchServiceToken();
    }

    private Mono<TokenResponse> refreshServiceToken() {
        TokenResponse currentToken = serviceToken.get();
        if (currentToken == null || currentToken.getRefreshToken() == null) {
            return Mono.error(new IllegalStateException("No refresh token available"));
        }
        return refreshToken(currentToken.getRefreshToken());
    }

    private void manageServiceTokenState(TokenResponse tokenResponse) {
        serviceToken.set(tokenResponse);
        serviceAccessTokenExpireMoment.set(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn() - 10));
        serviceRefreshTokenExpireMoment.set(LocalDateTime.now().plusSeconds(tokenResponse.getRefreshExpiresIn() - 10));
        log.info("Service token state updated");
    }
}
