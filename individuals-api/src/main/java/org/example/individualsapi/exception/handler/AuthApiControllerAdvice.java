package org.example.individualsapi.exception.handler;

import org.example.individualsapi.exception.KeycloakApiException;
import org.example.individualsapi.model.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class AuthApiControllerAdvice {

    @ExceptionHandler(KeycloakApiException.class)
    public ResponseEntity<?> handleKeycloakApiException(KeycloakApiException ex) {
        ErrorResponse errorResponse = ex.getErrorResponse();

        return ResponseEntity
                .status(HttpStatus.valueOf(errorResponse.getStatus()))
                .body(errorResponse);
    }
}
