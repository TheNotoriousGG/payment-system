package org.example.personservice.controller;

import org.example.personservice.model.dto.UserRegistrationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class PersonController implements PersonApi{

    @Override
    public Mono<ResponseEntity<String>> personRegistrationPost(Mono<UserRegistrationRequest> userRegistrationRequest, ServerWebExchange exchange) {
        return null;
    }
}
