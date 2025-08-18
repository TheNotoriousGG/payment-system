package org.example.individualsapi.it;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.example.individualsapi.config.TestKeycloakConfig;
import org.example.individualsapi.model.KeycloakUserResponse;
import org.example.individualsapi.model.dto.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.individualsapi.utils.TestConstants.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@Import(TestKeycloakConfig.class)
class AuthApiIntegrationTests {

    @Container
    static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:22.0.5")
            .withExposedPorts(8080)
            .withRealmImportFile("/test-realm.json")
            .withAdminUsername("admin")
            .withAdminPassword("admin")
            .waitingFor(Wait.defaultWaitStrategy());

    @DynamicPropertySource
    static void registerKeycloakProperties(DynamicPropertyRegistry registry) {
        registry.add("keycloak.url", keycloak::getAuthServerUrl);
    }

    @Autowired
    private WebTestClient webTestClient;

    @Value("${keycloak.realm_name}")
    private String realmName;

    @Value("${keycloak.client_id}")
    private String clientId;

    @Value("${keycloak.client_secret}")
    private String clientSecret;

    @Value("${keycloak.admin_username}")
    private String adminUsername;

    @Value("${keycloak.admin_password}")
    private String adminPassword;

    @BeforeEach
    void setUp() {
        webTestClient = webTestClient.mutate()
                .responseTimeout(Duration.ofMinutes(2))
                .build();
        
        cleanupUsers();
    }

    @Test
    @DisplayName("Должен зарегистрировать нового пользователя в keycloak")
    void givenValidUserRegistrationRequest_whenPostAuthRegistration_thenReturnsTokenResponse() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest()
                .email(JOHN.EMAIL)
                .password(JOHN.PASSWORD)
                .confirmPassword(JOHN.PASSWORD);

        // When
        var userRegistrationResponse = registerUser(request);

        // Then
        userRegistrationResponse
                .expectStatus().isCreated()
                .expectBody(TokenResponse.class)
                .consumeWith(result -> {
                    TokenResponse tokenResponse = result.getResponseBody();
                    assertThat(tokenResponse).isNotNull();
                    assertThat(tokenResponse.getAccessToken()).isNotNull();
                    assertThat(tokenResponse.getRefreshToken()).isNotNull();
                    assertThat(tokenResponse.getExpiresIn()).isPositive();
                    assertThat(tokenResponse.getRefreshExpiresIn()).isPositive();

                    webTestClient
                            .get()
                            .uri("/v1/auth/me")
                            .header("Authorization", "Bearer " + tokenResponse.getAccessToken())
                            .exchange()
                            .expectStatus().isOk()
                            .expectBody(UserInfoResponse.class)
                            .consumeWith(meResult -> {
                                UserInfoResponse userInfo = meResult.getResponseBody();
                                assertThat(userInfo).isNotNull();
                                assertThat(userInfo.getEmail()).isEqualTo(JOHN.EMAIL);
                            });
                });
    }

    @Test
    @DisplayName("Должен вернуть ошибку при попытке зарегистрировать существующего пользователя")
    void givenInvalidUserRegistrationRequest_whenPostAuthRegistration_thenReturnsErrorResponse() {
        //Given
        UserRegistrationRequest request = new UserRegistrationRequest()
                .email(JOHN.EMAIL)
                .password(JOHN.PASSWORD)
                .confirmPassword(JOHN.PASSWORD);

        //When
        var firstRegistrationResponse = registerUser(request);
        var secondUserRegistrationResponse = registerUser(request);

        //Then
        firstRegistrationResponse.expectStatus().isCreated();
        secondUserRegistrationResponse.expectStatus().is4xxClientError();
    }

    @Test
    @DisplayName("Должен успешно залогинить пользователя")
    void givenValidUserLoginData_whenPostAuthLogin_thenReturnsTokenResponse() {

        //Given
        UserRegistrationRequest request = new UserRegistrationRequest()
                .email(JOHN.EMAIL)
                .password(JOHN.PASSWORD)
                .confirmPassword(JOHN.PASSWORD);

        registerUser(request);

        //When

        var tokenResponse = webTestClient.post()
                .uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UserLoginRequest(JOHN.EMAIL, JOHN.PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenResponse.class)
                .returnResult().getResponseBody();

        //Then

        assertThat(tokenResponse).isNotNull();
        assertThat(tokenResponse.getAccessToken()).isNotNull();
        assertThat(tokenResponse.getRefreshToken()).isNotNull();
        assertThat(tokenResponse.getExpiresIn()).isPositive();
        assertThat(tokenResponse.getRefreshExpiresIn()).isPositive();

        webTestClient
                .get()
                .uri("/v1/auth/me")
                .header("Authorization", "Bearer " + tokenResponse.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserInfoResponse.class)
                .consumeWith(meResult -> {
                    UserInfoResponse userInfo = meResult.getResponseBody();
                    assertThat(userInfo).isNotNull();
                    assertThat(userInfo.getEmail()).isEqualTo(JOHN.EMAIL);
                });

    }

    @Test
    @DisplayName("Должен вернуть 401 при невалидных данных пользователя")
    void givenInvalidPassword_whenLogin_thenReturnsUnauthorized() {
        // Given
        UserLoginRequest request = new UserLoginRequest(JOHN.EMAIL, "wrongpassword");

        // When & Then
        webTestClient.post()
                .uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Должен вернуть данные пользователя при валидных данных")
    void givenValidCreds_WhenAuthMe_ThenReturnsOkWithUserData() {
        //Given
        UserRegistrationRequest request = new UserRegistrationRequest()
                .email(JOHN.EMAIL)
                .password(JOHN.PASSWORD)
                .confirmPassword(JOHN.PASSWORD);

        registerUser(request);

        var tokenResponse = webTestClient.post()
                .uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UserLoginRequest(JOHN.EMAIL, JOHN.PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenResponse.class)
                .returnResult().getResponseBody();

        assertThat(tokenResponse).isNotNull();
        assertThat(tokenResponse.getAccessToken()).isNotNull();

        //When
        var userInfo = webTestClient.get()
                .uri("/v1/auth/me")
                .header("Authorization", "Bearer " + tokenResponse.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserInfoResponse.class)
                .returnResult().getResponseBody();

        //Then
        assertThat(userInfo).isNotNull();
        assertThat(userInfo.getEmail()).isEqualTo(JOHN.EMAIL);
    }

    @Test
    @DisplayName("Должен вернуть ошибку с невалидными данными пользователя")
    void givenInValidCreds_WhenAuthMe_ThenReturnsUserNotFound() {
        //Given
        var invalidAccess = "some-invalid-access-token";

        //When
        var userInfo = webTestClient.get()
                .uri("/v1/auth/me")
                .header("Authorization", "Bearer " + invalidAccess)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(UserInfoResponse.class)
                .returnResult().getResponseBody();

        //Then
        assertThat(userInfo).isNull();
    }

    private WebTestClient.ResponseSpec registerUser(UserRegistrationRequest userRegistrationRequest) {
        return webTestClient
                .post()
                .uri("/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userRegistrationRequest)
                .exchange();
    }

    private void cleanupUsers() {
        try {
            var tokenResponse = webTestClient.post()
                    .uri(keycloak.getAuthServerUrl() + "/realms/" + realmName + "/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(String.format("grant_type=password&username=%s&password=%s&client_id=%s&client_secret=%s",
                            adminUsername, adminPassword, clientId, clientSecret))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(TokenResponse.class)
                    .returnResult().getResponseBody();

            String adminToken = tokenResponse.getAccessToken();

            webTestClient.get()
                    .uri(keycloak.getAuthServerUrl() + "/admin/realms/" + realmName + "/users")
                    .header("Authorization", "Bearer " + adminToken)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(KeycloakUserResponse.class)
                    .consumeWith(result -> {
                        if (result.getResponseBody() != null) {
                            result.getResponseBody().stream()
                                    .filter(user -> !adminUsername.equals(user.getUsername()))
                                    .forEach(user -> {
                                        webTestClient.delete()
                                                .uri(keycloak.getAuthServerUrl() + "/admin/realms/" + realmName + "/users/" + user.getId())
                                                .header("Authorization", "Bearer " + adminToken)
                                                .exchange()
                                                .expectStatus().isNoContent();
                                    });
                        }
                    });
        } catch (Exception ignored) {}
    }
}