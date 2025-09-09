package org.example.individualsapi;

import org.example.individualsapi.config.KeycloakProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(KeycloakProperties.class)
public class IndividualsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(IndividualsApiApplication.class, args);
    }

}
