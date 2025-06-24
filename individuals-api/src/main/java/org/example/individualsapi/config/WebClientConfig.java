package org.example.individualsapi.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    @Value("${keycloak.url}")
    private String keycloakUrl;

    @Bean
    @Qualifier("keycloakClient")
    WebClient getKeyCloakClient() {
        return WebClient.builder()
                .baseUrl(keycloakUrl)
                .build();
    }
}
