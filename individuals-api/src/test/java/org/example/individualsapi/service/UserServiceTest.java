package org.example.individualsapi.service;

import org.example.individualsapi.mapper.UserMapper;
import org.example.individualsapi.model.KeycloakUserResponse;
import org.example.individualsapi.model.KeycloakRoleMappingResponse;
import org.example.individualsapi.model.dto.TokenResponse;
import org.example.individualsapi.model.dto.UserInfoResponse;
import org.example.individualsapi.model.dto.UserRegistrationRequest;
import org.example.individualsapi.util.AuthContextUtil;
import org.example.individualsapi.util.RequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.example.individualsapi.utils.TestConstants.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private KeycloakClient keycloakClient;

    @InjectMocks
    private UserService userService;

    private UserRegistrationRequest userRegistrationRequest;
    private TokenResponse tokenResponse;
    private KeycloakUserResponse keycloakUserResponse;
    private KeycloakRoleMappingResponse roleMappingResponse;
    private UserInfoResponse userInfoResponse;

    @BeforeEach
    void setUp() {
        userRegistrationRequest = new UserRegistrationRequest()
                .email(JOHN.EMAIL)
                .password(JOHN.PASSWORD)
                .confirmPassword(JOHN.PASSWORD);

        tokenResponse = new TokenResponse()
                .accessToken(ACCESS_TOKEN)
                .refreshToken(REFRESH_TOKEN)
                .expiresIn(EXPIRES_IN)
                .refreshExpiresIn(REFRESH_EXPIRES_IN);

        keycloakUserResponse = new KeycloakUserResponse();
        keycloakUserResponse.setId(JOHN.USER_ID);
        keycloakUserResponse.setEmail(JOHN.EMAIL);

        roleMappingResponse = new KeycloakRoleMappingResponse();
        
        userInfoResponse = new UserInfoResponse()
                .email(JOHN.EMAIL);
    }

    @Test
    @DisplayName("Должен создать пользователя и вернуть токен при валидном запросе регистрации")
    void givenValidRegistrationRequest_whenUserRegistration_thenCreatesUserAndReturnsToken() {
        // Given
        when(tokenService.getServiceToken()).thenReturn(Mono.just(SERVICE_TOKEN));
        when(keycloakClient.addNewKeycloakUser(anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());
        when(tokenService.getUserToken(JOHN.USERNAME, JOHN.PASSWORD))
                .thenReturn(Mono.just(tokenResponse));

        try (var mockedRequestValidator = mockStatic(RequestValidator.class)) {
            mockedRequestValidator.when(() -> RequestValidator.validateUserRegistrationRequest(any()))
                    .thenReturn(Mono.just(userRegistrationRequest));

            // When
            StepVerifier.create(userService.userRegistration(Mono.just(userRegistrationRequest)))
                    .expectNext(tokenResponse)
                    .verifyComplete();

            // Then
            verify(tokenService).getServiceToken();
            verify(keycloakClient).addNewKeycloakUser(JOHN.EMAIL, JOHN.PASSWORD, SERVICE_TOKEN);
            verify(tokenService).getUserToken(JOHN.USERNAME, JOHN.PASSWORD);
            verifyNoMoreInteractions(tokenService, keycloakClient);
            verifyNoInteractions(userMapper);
        }
    }

    @Test
    @DisplayName("Должен вернуть токен при валидных учетных данных для входа")
    void givenValidCredentials_whenUserLogin_thenReturnsTokenResponse() {
        // Given
        when(tokenService.getUserToken(JOHN.USERNAME, JOHN.PASSWORD))
                .thenReturn(Mono.just(tokenResponse));

        // When
        StepVerifier.create(userService.userLogin(JOHN.EMAIL, JOHN.PASSWORD))
                .expectNext(tokenResponse)
                .verifyComplete();

        // Then
        verify(tokenService).getUserToken(JOHN.USERNAME, JOHN.PASSWORD);
        verifyNoMoreInteractions(tokenService);
        verifyNoInteractions(keycloakClient, userMapper);
    }

    @Test
    @DisplayName("Должен вернуть новый токен при валидном refresh токене")
    void givenValidRefreshToken_whenRefreshToken_thenReturnsNewTokenResponse() {
        // Given
        when(tokenService.refreshToken(REFRESH_TOKEN))
                .thenReturn(Mono.just(tokenResponse));

        // When
        StepVerifier.create(userService.refreshToken(REFRESH_TOKEN))
                .expectNext(tokenResponse)
                .verifyComplete();

        // Then
        verify(tokenService).refreshToken(REFRESH_TOKEN);
        verifyNoMoreInteractions(tokenService);
        verifyNoInteractions(keycloakClient, userMapper);
    }

    @Test
    @DisplayName("Должен вернуть информацию пользователя с ролями для аутентифицированного пользователя")
    void givenAuthenticatedUser_whenGetAuthUserInfo_thenReturnsUserInfoWithRoles() {
        // Given
        when(tokenService.getServiceToken()).thenReturn(Mono.just(SERVICE_TOKEN));
        when(keycloakClient.getUserInfo(JOHN.USER_ID, SERVICE_TOKEN))
                .thenReturn(Mono.just(keycloakUserResponse));
        when(keycloakClient.getUserRoles(JOHN.USER_ID, SERVICE_TOKEN))
                .thenReturn(Mono.just(roleMappingResponse));
        when(keycloakClient.extractClientRoles(roleMappingResponse))
                .thenReturn(USER_ROLES);
        when(userMapper.toUserInfoResponse(keycloakUserResponse))
                .thenReturn(userInfoResponse);
        
        try (var mockedAuthContextUtil = mockStatic(AuthContextUtil.class)) {
            mockedAuthContextUtil.when(AuthContextUtil::getCurrentUserId)
                    .thenReturn(Mono.just(JOHN.USER_ID));

            // When
            StepVerifier.create(userService.getAuthUserInfo())
                    .expectNextMatches(response -> response.getRoles().equals(USER_ROLES))
                    .verifyComplete();

            // Then
            verify(tokenService).getServiceToken();
            verify(keycloakClient).getUserInfo(JOHN.USER_ID, SERVICE_TOKEN);
            verify(keycloakClient).getUserRoles(JOHN.USER_ID, SERVICE_TOKEN);
            verify(keycloakClient).extractClientRoles(roleMappingResponse);
            verify(userMapper).toUserInfoResponse(keycloakUserResponse);
            verifyNoMoreInteractions(tokenService, keycloakClient, userMapper);
        }
    }

    @Test
    @DisplayName("Должен вернуть ошибку при сбое в токен сервисе при входе")
    void givenTokenServiceFails_whenUserLogin_thenReturnsError() {
        // Given
        when(tokenService.getUserToken(JOHN.USERNAME, JOHN.PASSWORD))
                .thenReturn(Mono.error(new RuntimeException(TOKEN_SERVICE_ERROR_MESSAGE)));

        // When
        StepVerifier.create(userService.userLogin(JOHN.EMAIL, JOHN.PASSWORD))
                .expectError(RuntimeException.class)
                .verify();

        // Then
        verify(tokenService).getUserToken(JOHN.USERNAME, JOHN.PASSWORD);
        verifyNoMoreInteractions(tokenService);
        verifyNoInteractions(keycloakClient, userMapper);
    }

    @Test
    @DisplayName("Должен вернуть ошибку при сбое получения сервисного токена")
    void givenServiceTokenFails_whenGetAuthUserInfo_thenReturnsError() {
        // Given
        when(tokenService.getServiceToken())
                .thenReturn(Mono.error(new RuntimeException(SERVICE_TOKEN_ERROR_MESSAGE)));
        
        try (var mockedAuthContextUtil = mockStatic(AuthContextUtil.class)) {
            mockedAuthContextUtil.when(AuthContextUtil::getCurrentUserId)
                    .thenReturn(Mono.just(JOHN.USER_ID));

            // When
            StepVerifier.create(userService.getAuthUserInfo())
                    .expectError(RuntimeException.class)
                    .verify();

            // Then
            verify(tokenService).getServiceToken();
            verifyNoMoreInteractions(tokenService);
            verifyNoInteractions(keycloakClient, userMapper);
        }
    }
}