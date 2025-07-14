package org.example.individualsapi.config;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Getter
@Validated
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "keycloak")
public final class KeycloakProperties {

    @NotBlank
    private final String url;

    @NotBlank
    private final String realmName;

    @NotBlank
    private final String clientId;

    @NotBlank
    private final String clientSecret;

    @NotBlank
    private final String adminUsername;

    @NotBlank
    private final String adminPassword;

    public String getRealmUrl() {
        return url + "/realms/" + realmName;
    }

    public String getAdminUrl() {
        return url + "/admin/realms/" + realmName;
    }

    public String getTokenUrl() {
        return getRealmUrl() + "/protocol/openid-connect/token";
    }

    public String getUserInfoUrl() {
        return getRealmUrl() + "/protocol/openid-connect/userinfo";
    }

    public String getAdminUsersUrl() {
        return getAdminUrl() + "/users";
    }
}