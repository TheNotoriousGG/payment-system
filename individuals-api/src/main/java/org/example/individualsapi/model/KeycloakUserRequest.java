package org.example.individualsapi.model;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class KeycloakUserRequest {
    private String username;
    private String email;
    private boolean enabled;
    private String firstName;
    private String lastName;
    private boolean emailVerified;
    private List<KeycloakUserCredential> credentials;
}
