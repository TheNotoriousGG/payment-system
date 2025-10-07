package org.example.personservice.service;

import lombok.RequiredArgsConstructor;
import org.example.personservice.entity.Country;
import org.example.personservice.repository.CountryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CountriesService {

    private final CountryRepository repository;

    public void saveAll(List<Country> countries) {
        repository.saveAll(countries);
    }

    public Long getCount() {
        return repository.count();
    }
}
