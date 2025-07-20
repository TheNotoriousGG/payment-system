package org.example.individualsapi.util;

import org.example.individualsapi.exception.RequestValidationException;
import org.example.individualsapi.model.dto.UserRegistrationRequest;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class RequestValidator {

    public static Mono<UserRegistrationRequest> validateUserRegistrationRequest(UserRegistrationRequest request) {
        if (!Objects.equals(request.getPassword(), request.getConfirmPassword())) {
            return Mono.error(new RequestValidationException("Passwords do not match"));
        }
        return Mono.just(request);
    }
}
