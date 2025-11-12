package org.example.individualsapi.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.personapi.api.PersonServiceApiClient;
import org.example.personapi.dto.IndividualWriteDto;
import org.example.personapi.dto.IndividualWriteResponseDto;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonServiceApiClient personApiClient;

    @WithSpan("personService.register")
    public Mono<IndividualWriteResponseDto> register(IndividualWriteDto request) {
        return Mono.fromCallable(() -> personApiClient.registration(request))
                .mapNotNull(HttpEntity::getBody)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(t -> log.info("Person registered id = [{}]", t.getId()));
    }

    @WithSpan("personService.compensateRegistration")
    public Mono<Void> compensateRegistration(String id) {
        return Mono.fromRunnable(() -> personApiClient.compensateRegistration(UUID.fromString(id)))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

}
