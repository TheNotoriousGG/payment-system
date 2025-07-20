package org.example.individualsapi.exception.handler;

import org.example.individualsapi.exception.KeycloakApiException;
import org.example.individualsapi.model.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@ControllerAdvice
public class AuthApiControllerAdvice {

    @ExceptionHandler(KeycloakApiException.class)
    public ResponseEntity<?> handleKeycloakApiException(KeycloakApiException ex) {
        ErrorResponse errorResponse = ex.getErrorResponse();

        return ResponseEntity
                .status(HttpStatus.valueOf(errorResponse.getStatus()))
                .body(errorResponse);
    }

    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<?> handleWebClientRequestException(WebClientRequestException ex) {

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        errorResponse.setError("Service Unavailable please try again later.");

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse);
    }
}
