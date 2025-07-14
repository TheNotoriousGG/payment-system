package org.example.individualsapi.util;

import org.example.individualsapi.model.KeycloakUserCredential;
import org.example.individualsapi.model.KeycloakUserRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

import static org.example.individualsapi.util.StrUtils.*;

public class RequestBuilder {

    public static KeycloakUserRequest buildKeycloakUserRequest(String email, String password) {
        String username = usernameFromEmail(email);
        KeycloakUserCredential userCredential = new KeycloakUserCredential("password",password,false);

        return KeycloakUserRequest.builder()
                .email(email)
                .username(username)
                .enabled(true)
                .emailVerified(true)
                .firstName(username)
                .lastName(username)
                .credentials(List.of(userCredential))
                .build();
    }

    public static MultiValueMap<String, String> buildGetTokenRequestFormData(String username, String password, String clientId, String clientSecret) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("grant_type", "password");
        formData.add("username", username);
        formData.add("password", password);
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);

        return formData;
    }

    public static MultiValueMap<String, String> buildRefreshTokenRequestFormData(String refreshToken, String clientId, String clientSecret) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("grant_type", "refresh_token");
        formData.add("refresh_token", refreshToken);
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);

        return formData;
    }
}
