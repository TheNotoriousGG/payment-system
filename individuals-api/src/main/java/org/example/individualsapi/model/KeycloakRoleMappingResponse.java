package org.example.individualsapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeycloakRoleMappingResponse {

    @JsonProperty("realmMappings")
    private List<KeycloakRoleMapping> realmMappings;

    @JsonProperty("clientMappings")
    private Map<String, KeycloakClientMapping> clientMappings;
}