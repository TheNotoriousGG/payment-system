package org.example.individualsapi.config;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.wait.strategy.Wait;

@TestConfiguration
public class TestKeycloakConfig {

    @Value("${keycloak.testcontainers.image}")
    private String keycloakImage;

    @Value("${keycloak.testcontainers.realm_import_file}")
    private String realmImportFile;

    @Value("${keycloak.admin_username}")
    private String containerAdminUsername;

    @Value("${keycloak.admin_password}")
    private String containerAdminPassword;

    @Value("${keycloak.testcontainers.port}")
    private int keycloakPort;

    @Bean
    public KeycloakContainer keycloakContainer() {
        return new KeycloakContainer(keycloakImage)
                .withExposedPorts(keycloakPort)
                .withRealmImportFile(realmImportFile)
                .withAdminUsername(containerAdminUsername)
                .withAdminPassword(containerAdminPassword)
                .waitingFor(Wait.defaultWaitStrategy());
    }
}
