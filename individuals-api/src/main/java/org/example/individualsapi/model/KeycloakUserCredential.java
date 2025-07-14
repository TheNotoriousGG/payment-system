package org.example.individualsapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakUserCredential {
    private String type;
    private String value;
    private boolean temporary;
}
