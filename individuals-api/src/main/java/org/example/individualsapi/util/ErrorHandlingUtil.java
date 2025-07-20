package org.example.individualsapi.util;

import org.example.individualsapi.exception.KeycloakApiException;
import org.example.individualsapi.model.KeycloakError;
import org.example.individualsapi.model.dto.ErrorResponse;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public class ErrorHandlingUtil {

    public static Function<ClientResponse, Mono<? extends Throwable>> keycloakHttpErrorMapper() {
        return clientResponse ->
                clientResponse.bodyToMono(KeycloakError.class)
                        .flatMap(error -> {
                            String message = String.format("%s: %s", error.getError(), error.getErrorDescription());

                            ErrorResponse errorResponse = new ErrorResponse();
                            errorResponse.setStatus(clientResponse.statusCode().value());
                            errorResponse.setError(message);

                            return Mono.error(new KeycloakApiException(errorResponse, message));
                        });
    }
}
