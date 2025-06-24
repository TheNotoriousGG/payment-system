package org.example.individualsapi.exception;

import lombok.Getter;
import org.example.individualsapi.model.dto.ErrorResponse;

@Getter
public class KeycloakApiException extends RuntimeException {
    private final ErrorResponse errorResponse;

    public KeycloakApiException(ErrorResponse errorResponse,String message) {
        super(message);
        this.errorResponse = errorResponse;
    }
}
