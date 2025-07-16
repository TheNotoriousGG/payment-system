package org.example.individualsapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakClientMapping {
    private String id;
    private String client;

    @JsonProperty("mappings")
    private List<KeycloakRoleMapping> mappings;
}