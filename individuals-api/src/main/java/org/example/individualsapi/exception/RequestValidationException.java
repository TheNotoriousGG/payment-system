package org.example.individualsapi.exception;

public class RequestValidationException extends RuntimeException {
    
    public RequestValidationException(String message) {
        super(message);
    }
}
