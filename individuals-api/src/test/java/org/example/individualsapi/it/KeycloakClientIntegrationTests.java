package org.example.individualsapi.it;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public class KeycloakClientIntegrationTests {

    @Container
    static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:22.0.5")
            .withExposedPorts(8080)
            .withRealmImportFile("/test-realm.json")
            .withAdminUsername("admin")
            .withAdminPassword("admin")
            .waitingFor(Wait.defaultWaitStrategy());

    @DynamicPropertySource
    static void registerKeycloakProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloak.getAuthServerUrl() + "/realms/test-realm");
    }

    @Test
    void contextLoads() {
        Assert.assertNotNull(keycloak);
        System.out.println("Keycloak URL: " + keycloak.getAuthServerUrl());
    }
}

