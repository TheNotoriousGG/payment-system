package org.example.individualsapi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
@Slf4j
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
