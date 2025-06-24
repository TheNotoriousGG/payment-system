package org.example.individualsapi.service;

import lombok.RequiredArgsConstructor;
import org.example.individualsapi.exception.RequestValidationException;
import org.example.individualsapi.model.dto.TokenResponse;
import org.example.individualsapi.model.dto.UserRegistrationRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private final KeycloakService keycloakService;

    public Mono<TokenResponse> userRegistration(Mono<UserRegistrationRequest> userRegistrationRequest) {

        return userRegistrationRequest
                .flatMap(request -> {
                    if (!Objects.equals(request.getPassword(), request.getConfirmPassword())) {
                        return Mono.error(new RequestValidationException("Passwords do not match"));
                    }

                    String email = request.getEmail();
                    String password = request.getPassword();
                    String username = email.substring(0, email.indexOf("@"));

                    Mono<TokenResponse> adminToken = keycloakService.getAdminToken();

                    return adminToken.flatMap( tokenResponse ->
                            keycloakService.addNewUser(
                                    email,
                                    password,
                                    username,
                                    tokenResponse.getAccessToken()
                            )
                    ).then(
                            keycloakService.getUserToken(username,password)
                    );
                });
    }
}
