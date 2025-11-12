package org.example.personservice.service;

import org.example.personapi.dto.*;
import org.example.personservice.entity.Address;
import org.example.personservice.entity.Country;
import org.example.personservice.entity.Individual;
import org.example.personservice.entity.User;
import org.example.personservice.exception.PersonException;
import org.example.personservice.mapper.IndividualMapper;
import org.example.personservice.repository.IndividualRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndividualServiceTest {

    @Mock
    private IndividualRepository individualRepository;

    @Mock
    private IndividualMapper individualMapper;

    @InjectMocks
    private IndividualService individualService;

    @Test
    @DisplayName("Должен вернуть id зарегистрированного пользователя")
    void register() {
        // given
        UUID expectedId = UUID.randomUUID();
        IndividualWriteDto individualWriteDto = new IndividualWriteDto(
                "test@example.com",
                "Gennadiy",
                "Parovozov",
                "010101",
                "9379992",
                "9379992",
                new AddressWriteDto()
        );

        User user = new User();
        user.setEmail("test@example.com");
        user.setFirstName("Gennadiy");
        user.setLastName("Parovozov");

        Individual individual = new Individual();
        individual.setPassportNumber("010101");
        individual.setPhoneNumber("9379992");
        individual.setUser(user);

        when(individualMapper.to(individualWriteDto)).thenReturn(individual);
        when(individualRepository.save(any(Individual.class))).thenAnswer(invocation -> {
            Individual savedIndividual = invocation.getArgument(0);
            savedIndividual.setId(expectedId);
            return savedIndividual;
        });

        // when
        IndividualWriteResponseDto result = individualService.register(individualWriteDto);

        // then
        assertNotNull(result);
        assertEquals(expectedId.toString(), result.getId());
        verify(individualMapper).to(individualWriteDto);
        verify(individualRepository).save(individual);
    }

    @Test
    @DisplayName("Должен вернуть список пользователей по email")
    void findByEmails() {
        // given
        List<String> emails = List.of("test1@example.com", "test2@example.com");

        Individual individual1 = createIndividual("test1@example.com");
        Individual individual2 = createIndividual("test2@example.com");
        List<Individual> individuals = List.of(individual1, individual2);

        IndividualDto dto1 = new IndividualDto();
        dto1.setEmail("test1@example.com");
        IndividualDto dto2 = new IndividualDto();
        dto2.setEmail("test2@example.com");
        List<IndividualDto> dtos = List.of(dto1, dto2);

        when(individualRepository.findAllByEmails(emails)).thenReturn(individuals);
        when(individualMapper.from(individuals)).thenReturn(dtos);

        // when
        IndividualPageDto result = individualService.findByEmails(emails);

        // then
        assertNotNull(result);
        assertEquals(2, result.getItems().size());
        assertEquals("test1@example.com", result.getItems().get(0).getEmail());
        assertEquals("test2@example.com", result.getItems().get(1).getEmail());
        verify(individualRepository).findAllByEmails(emails);
        verify(individualMapper).from(individuals);
    }

    @Test
    @DisplayName("Должен вернуть пользователя по id")
    void findById() {
        // given
        UUID id = UUID.randomUUID();
        Individual individual = createIndividual("test@example.com");
        individual.setId(id);

        IndividualDto dto = new IndividualDto();
        dto.setEmail("test@example.com");

        when(individualRepository.findById(id)).thenReturn(Optional.of(individual));
        when(individualMapper.from(individual)).thenReturn(dto);

        // when
        IndividualDto result = individualService.findById(id);

        // then
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        verify(individualRepository).findById(id);
        verify(individualMapper).from(individual);
    }

    @Test
    @DisplayName("Должен выбрасывать исключение когда пользователь не найден")
    void findById_notFound() {
        // given
        UUID id = UUID.randomUUID();
        when(individualRepository.findById(id)).thenReturn(Optional.empty());

        // when & then
        PersonException exception = assertThrows(
                PersonException.class,
                () -> individualService.findById(id)
        );

        assertTrue(exception.getMessage().contains("Individual not found by id"));
        verify(individualRepository).findById(id);
        verifyNoInteractions(individualMapper);
    }

    @Test
    @DisplayName("Должен выполнять soft удаление пользователя по id")
    void softDelete() {
        // given
        UUID id = UUID.randomUUID();

        // when
        individualService.softDelete(id);

        // then
        verify(individualRepository).softDelete(id);
    }

    @Test
    @DisplayName("Должен выполнять hard удаление пользователя по id")
    void hardDelete() {
        // given
        UUID id = UUID.randomUUID();
        Individual individual = createIndividual("test@example.com");
        individual.setId(id);

        when(individualRepository.findById(id)).thenReturn(Optional.of(individual));

        // when
        individualService.hardDelete(id);

        // then
        verify(individualRepository).findById(id);
        verify(individualRepository).delete(individual);
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при жестком удалении, когда пользователь не найден")
    void hardDelete_notFound() {
        // given
        UUID id = UUID.randomUUID();
        when(individualRepository.findById(id)).thenReturn(Optional.empty());

        // when & then
        PersonException exception = assertThrows(
                PersonException.class,
                () -> individualService.hardDelete(id)
        );

        assertTrue(exception.getMessage().contains("Individual not found by id"));
        verify(individualRepository).findById(id);
        verify(individualRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Должен обновлять пользователя и возвращать id")
    void update() {
        // given
        UUID id = UUID.randomUUID();
        IndividualWriteDto writeDto = new IndividualWriteDto(
                "test@example.com",
                "Updated",
                "Person",
                "999999",
                "8888888",
                "9379992",
                new AddressWriteDto()
        );

        Individual individual = createIndividual("test@example.com");
        individual.setId(id);

        when(individualRepository.findById(id)).thenReturn(Optional.of(individual));
        doNothing().when(individualMapper).update(individual, writeDto);
        when(individualRepository.save(individual)).thenReturn(individual);

        // when
        IndividualWriteResponseDto result = individualService.update(id, writeDto);

        // then
        assertNotNull(result);
        assertEquals(id.toString(), result.getId());
        verify(individualRepository).findById(id);
        verify(individualMapper).update(individual, writeDto);
        verify(individualRepository).save(individual);
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при обновлении, когда пользователь не найден")
    void update_notFound() {
        // given
        UUID id = UUID.randomUUID();
        IndividualWriteDto writeDto = new IndividualWriteDto(
                "test@example.com",
                "Updated",
                "Person",
                "999999",
                "8888888",
                "9379992",
                new AddressWriteDto()
        );

        when(individualRepository.findById(id)).thenReturn(Optional.empty());

        // when & then
        PersonException exception = assertThrows(
                PersonException.class,
                () -> individualService.update(id, writeDto)
        );

        assertTrue(exception.getMessage().contains("Individual not found by id"));
        verify(individualRepository).findById(id);
        verify(individualMapper, never()).update(any(), any());
        verify(individualRepository, never()).save(any());
    }

    private Individual createIndividual(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");

        Address address = new Address();
        address.setAddress("Test Street 123");
        address.setCity("Test City");
        address.setZipCode("12345");

        Country country = new Country();
        country.setName("Test Country");

        address.setCountry(country);
        user.setAddress(address);

        Individual individual = new Individual();
        individual.setUser(user);
        individual.setPassportNumber("123456");
        individual.setPhoneNumber("88001234567");
        individual.setActive(true);

        return individual;
    }
}