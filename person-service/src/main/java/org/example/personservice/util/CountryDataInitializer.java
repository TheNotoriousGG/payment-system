package org.example.personservice.util;

import lombok.RequiredArgsConstructor;
import org.example.personservice.model.entity.Country;
import org.example.personservice.service.CountriesService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CountryDataInitializer implements CommandLineRunner {

    private final CountriesService countriesService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        if (countriesService.getCount() == 0) {

            List<Country> countriesDictionary = List.of(
                    Country.builder().name("Russia").alpha2("RU").alpha3("RUS").created(LocalDateTime.now()).updated(LocalDateTime.now()).status("ACTIVE").build(),
                    Country.builder().name("England").alpha2("EN").alpha3("ENG").created(LocalDateTime.now()).updated(LocalDateTime.now()).status("ACTIVE").build(),
                    Country.builder().name("United States Of America").alpha2("US").alpha3("USA").created(LocalDateTime.now()).updated(LocalDateTime.now()).status("ACTIVE").build()
            );
            countriesService.saveAll(countriesDictionary);
        }
    }
}
