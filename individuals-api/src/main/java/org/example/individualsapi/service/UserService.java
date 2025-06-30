package org.example.individualsapi.service;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.example.individualsapi.exception.RequestValidationException;
import org.example.individualsapi.mapper.UserMapper;
import org.example.individualsapi.model.dto.TokenResponse;
import org.example.individualsapi.model.dto.UserInfoResponse;
import org.example.individualsapi.model.dto.UserRegistrationRequest;
import org.example.individualsapi.util.AuthContextUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static org.example.individualsapi.util.StrUtils.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final KeycloakService keycloakService;
    private final UserMapper userMapper;

    public Mono<TokenResponse> userRegistration(Mono<UserRegistrationRequest> userRegistrationRequest) {

        return userRegistrationRequest
                .flatMap(request -> {
                    if (!Objects.equals(request.getPassword(), request.getConfirmPassword())) {
                        return Mono.error(new RequestValidationException("Passwords do not match"));//todo
                    }

                    String email = request.getEmail();
                    String password = request.getPassword();
                    String username = usernameFromEmail(email);

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

    public Mono<TokenResponse> userLogin(String email, String password) {
        return keycloakService.getUserToken(usernameFromEmail(email), password);
    }

    public Mono<TokenResponse> refreshToken(@NotNull String refreshToken) {
        return keycloakService.refreshToken(refreshToken);
    }

    public Mono<UserInfoResponse> getAuthUserInfoFromContext() {
        return AuthContextUtil.getCurrentUserId()
                .flatMap(userId ->
                        keycloakService.getAdminToken()
                                .map(TokenResponse::getAccessToken)
                                .flatMap(adminToken ->
                                        keycloakService.getUserInfo(userId, adminToken)
                                                .map(userMapper::toUserInfoResponse)
                                                .flatMap(userInfoResponse ->
                                                        AuthContextUtil.getUserRolesFromContext()
                                                                .map(roles -> {
                                                                    userInfoResponse.setRoles(roles);
                                                                    return userInfoResponse;
                                                                })
                                                )
                                )
                );
    }
}
