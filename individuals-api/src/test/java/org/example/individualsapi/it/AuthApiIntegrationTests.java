package org.example.individualsapi.it;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.example.individualsapi.model.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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

    @BeforeEach
    void setUp() {
        webTestClient = webTestClient.mutate()
                .responseTimeout(Duration.ofMinutes(2))
                .build();
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

        System.out.println();

    }

    private WebTestClient.ResponseSpec registerUser(UserRegistrationRequest userRegistrationRequest) {
        return webTestClient
                .post()
                .uri("/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userRegistrationRequest)
                .exchange();
    }
}