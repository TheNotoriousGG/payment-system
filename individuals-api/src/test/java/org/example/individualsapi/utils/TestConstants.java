package org.example.individualsapi.utils;

import java.util.List;

public class TestConstants {
    
    // Test Users
    public static final class JOHN {
        public static final String EMAIL = "john.doe@example.com";
        public static final String USERNAME = "john.doe";
        public static final String USER_ID = "john-user-id";
        public static final String PASSWORD = "john123";
    }
    
    public static final class DAVID {
        public static final String USERNAME = "david.smith";
        public static final String PASSWORD = "david123";
    }
    
    public static final class GENA {
        public static final String USERNAME = "gena.petrov";
        public static final String PASSWORD = "gena123";
    }
    
    // Tokens
    public static final String SERVICE_TOKEN = "service-token";
    public static final String ACCESS_TOKEN = "access-token";
    public static final String REFRESH_TOKEN = "refresh-token";
    public static final String INVALID_TOKEN = "invalid-token";
    
    // Token expiration
    public static final int EXPIRES_IN = 3600;
    public static final int REFRESH_EXPIRES_IN = 7200;
    
    // User roles
    public static final List<String> USER_ROLES = List.of("user", "admin");
    
    // Error messages
    public static final String KEYCLOAK_ERROR_MESSAGE = "Keycloak error";
    public static final String TOKEN_SERVICE_ERROR_MESSAGE = "Token service error";
    public static final String SERVICE_TOKEN_ERROR_MESSAGE = "Service token error";
    public static final String INVALID_REFRESH_TOKEN_ERROR_MESSAGE = "Invalid refresh token";
    public static final String KEYCLOAK_UNAVAILABLE_ERROR_MESSAGE = "Keycloak unavailable";
    
}