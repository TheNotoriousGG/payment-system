package org.example.individualsapi.config;

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

    public String getRealmBaseUrl() {
        return url + "/realms/" + realmName;
    }

    public String getAdminBaseUrl() {
        return url + "/admin/realms/" + realmName;
    }

    public String getTokenEndpoint() {
        return getRealmBaseUrl() + "/protocol/openid-connect/token";
    }

    public String getUserInfoEndpoint() {
        return getRealmBaseUrl() + "/protocol/openid-connect/userinfo";
    }

    public String getUsersEndpoint() {
        return getAdminBaseUrl() + "/users";
    }

    public String getUserEndpoint(String userId) {
        return getUsersEndpoint() + "/" + userId;
    }

    public String getUserRolesEndpoint(String userId) {
        return getUsersEndpoint() + "/" + userId + "/role-mappings";
    }
}