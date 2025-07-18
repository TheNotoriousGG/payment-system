package org.example.individualsapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakError {

    private String error;
    
    @JsonProperty("error_description")
    private String errorDescription;
}
