package org.example.individualsapi.service;

import org.example.individualsapi.model.dto.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;
import static org.example.individualsapi.service.TestConstants.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private KeycloakClient keycloakClient;

    @InjectMocks
    private TokenService tokenService;

    private TokenResponse tokenResponse;

    @BeforeEach
    void setUp() {
        tokenResponse = new TokenResponse()
                .accessToken(ACCESS_TOKEN)
                .refreshToken(REFRESH_TOKEN)
                .expiresIn(EXPIRES_IN)
                .refreshExpiresIn(REFRESH_EXPIRES_IN);
    }

    @Test
    @DisplayName("Должен вернуть токен пользователя при валидных учетных данных")
    void givenValidCredentials_whenGetUserToken_thenReturnsTokenResponse() {
        // Given
        when(keycloakClient.authenticateUser(DAVID.USERNAME, DAVID.PASSWORD))
                .thenReturn(Mono.just(tokenResponse));

        // When
        StepVerifier.create(tokenService.getUserToken(DAVID.USERNAME, DAVID.PASSWORD))
                .expectNext(tokenResponse)
                .verifyComplete();

        // Then
        verify(keycloakClient).authenticateUser(DAVID.USERNAME, DAVID.PASSWORD);
        verifyNoMoreInteractions(keycloakClient);
    }

    @Test
    @DisplayName("Должен вернуть новый токен при валидном refresh токене")
    void givenValidRefreshToken_whenRefreshToken_thenReturnsNewTokenResponse() {
        // Given
        when(keycloakClient.refreshToken(REFRESH_TOKEN))
                .thenReturn(Mono.just(tokenResponse));

        // When
        StepVerifier.create(tokenService.refreshToken(REFRESH_TOKEN))
                .expectNext(tokenResponse)
                .verifyComplete();

        // Then
        verify(keycloakClient).refreshToken(REFRESH_TOKEN);
        verifyNoMoreInteractions(keycloakClient);
    }

    @Test
    @DisplayName("Должен получить новый сервисный токен когда его нет")
    void givenNoServiceToken_whenGetServiceToken_thenFetchesNewToken() {
        // Given
        when(keycloakClient.fetchServiceToken())
                .thenReturn(Mono.just(tokenResponse));

        // When
        StepVerifier.create(tokenService.getServiceToken())
                .expectNext(ACCESS_TOKEN)
                .verifyComplete();

        // Then
        verify(keycloakClient).fetchServiceToken();
        verifyNoMoreInteractions(keycloakClient);
    }

    @Test
    @DisplayName("Должен вернуть ошибку при ошибке Keycloak при получении токена пользователя")
    void givenKeycloakError_whenGetUserToken_thenReturnsError() {
        // Given
        when(keycloakClient.authenticateUser(GENA.USERNAME, GENA.PASSWORD))
                .thenReturn(Mono.error(new RuntimeException(KEYCLOAK_ERROR_MESSAGE)));

        // When
        StepVerifier.create(tokenService.getUserToken(GENA.USERNAME, GENA.PASSWORD))
                .expectError(RuntimeException.class)
                .verify();

        // Then
        verify(keycloakClient).authenticateUser(GENA.USERNAME, GENA.PASSWORD);
        verifyNoMoreInteractions(keycloakClient);
    }

    @Test
    @DisplayName("Должен вернуть ошибку при невалидном refresh токене")
    void givenInvalidRefreshToken_whenRefreshToken_thenReturnsError() {
        // Given
        when(keycloakClient.refreshToken(INVALID_TOKEN))
                .thenReturn(Mono.error(new RuntimeException(INVALID_REFRESH_TOKEN_ERROR_MESSAGE)));

        // When
        StepVerifier.create(tokenService.refreshToken(INVALID_TOKEN))
                .expectError(RuntimeException.class)
                .verify();

        // Then
        verify(keycloakClient).refreshToken(INVALID_TOKEN);
        verifyNoMoreInteractions(keycloakClient);
    }

    @Test
    @DisplayName("Должен вернуть ошибку при недоступности Keycloak для сервисного токена")
    void givenKeycloakUnavailable_whenGetServiceToken_thenReturnsError() {
        // Given
        when(keycloakClient.fetchServiceToken())
                .thenReturn(Mono.error(new RuntimeException(KEYCLOAK_UNAVAILABLE_ERROR_MESSAGE)));

        // When
        StepVerifier.create(tokenService.getServiceToken())
                .expectError(RuntimeException.class)
                .verify();

        // Then
        verify(keycloakClient).fetchServiceToken();
        verifyNoMoreInteractions(keycloakClient);
    }


}