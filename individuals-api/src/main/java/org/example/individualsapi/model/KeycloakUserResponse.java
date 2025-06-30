package org.example.individualsapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class KeycloakUserResponse {
    
    private String id;
    private String username;
    
    @JsonProperty("firstName")
    private String firstName;
    
    @JsonProperty("lastName")
    private String lastName;
    
    private String email;
    
    @JsonProperty("emailVerified")
    private Boolean emailVerified;
    
    @JsonProperty("createdTimestamp")
    private Long createdTimestamp;
    
    private Boolean enabled;
    private Boolean totp;
    
    @JsonProperty("disableableCredentialTypes")
    private List<String> disableableCredentialTypes;
    
    @JsonProperty("requiredActions")
    private List<String> requiredActions;
    
    @JsonProperty("notBefore")
    private Integer notBefore;
    
    private Map<String, Boolean> access;
}