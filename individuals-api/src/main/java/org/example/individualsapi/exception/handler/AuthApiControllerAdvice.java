package org.example.individualsapi.exception.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.individualsapi.exception.KeycloakApiException;
import org.example.individualsapi.model.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@ControllerAdvice
@Slf4j
public class AuthApiControllerAdvice {

    @ExceptionHandler(KeycloakApiException.class)
    public ResponseEntity<?> handleKeycloakApiException(KeycloakApiException ex) {
        log.error("Keycloak API exception: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = ex.getErrorResponse();

        return ResponseEntity
                .status(HttpStatus.valueOf(errorResponse.getStatus()))
                .body(errorResponse);
    }

    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<?> handleWebClientRequestException(WebClientRequestException ex) {
        log.error("WebClient request exception: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        errorResponse.setError("Service Unavailable please try again later.");

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        log.error("Unexpected exception: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.setError("Internal server error occurred.");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}
