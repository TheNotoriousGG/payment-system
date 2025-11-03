package org.example.personservice.IT;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.personapi.dto.AddressWriteDto;
import org.example.personapi.dto.IndividualDto;
import org.example.personapi.dto.IndividualWriteDto;
import org.example.personapi.dto.IndividualWriteResponseDto;
import org.example.personservice.config.AbstractIntegrationTest;
import org.example.personservice.service.IndividualService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class IndividualRestControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IndividualService individualService;

    @Test
    @Transactional
    @DisplayName("Успешная регистрация нового пользователя")
    void individual_registration() throws Exception {
        // Given
        IndividualWriteDto givenIndividual = createTestIndividualDto(
            "john.doe@example.com",
            "John",
            "Doe",
            "AB123456",
            "+1234567890"
        );

        // When
        IndividualWriteResponseDto response = registerIndividual(givenIndividual);

        IndividualDto individualFromDB = individualService.findById(UUID.fromString(response.getId()));

        // Then
        Assertions.assertNotNull(individualFromDB);
        Assertions.assertEquals(givenIndividual.getEmail(), individualFromDB.getEmail());
    }

    @Test
    @DisplayName("Должен вернуть ошибку при регистрации с невалидными данными")
    void individual_registration_with_invalid_data() throws Exception {
        // Given
        IndividualWriteDto invalidIndividual = new IndividualWriteDto();
        invalidIndividual.setEmail("invalid-email");

        // When & Then
        mockMvc.perform(post("/v1/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidIndividual)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Transactional
    @DisplayName("Успешное получение пользователя по Id")
    void findById_with_valid_id() throws Exception {
        // Given
        IndividualWriteDto givenIndividual = createTestIndividualDto(
            "find.test@example.com",
            "Find",
            "Test",
            "FT123456",
            "+2222222222"
        );

        IndividualWriteResponseDto response = registerIndividual(givenIndividual);

        // When
        IndividualDto individualFromDB = findIndividual(response.getId());

        // Then
        Assertions.assertNotNull(individualFromDB);
        Assertions.assertEquals(givenIndividual.getEmail(), individualFromDB.getEmail());
        Assertions.assertEquals(givenIndividual.getFirstName(), individualFromDB.getFirstName());
        Assertions.assertEquals(givenIndividual.getLastName(), individualFromDB.getLastName());
    }

    @Test
    @DisplayName("Должен вернуть 404 при вызове метода findById с не существующим id")
    void findById_with_invalid_id() throws Exception {
        // Given
        String nonExistentId = UUID.randomUUID().toString();

        // When & Then
        mockMvc.perform(get("/v1/persons/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @DisplayName("Успешное получение списка пользователей по email")
    void findAllByEmail_with_valid_emails() throws Exception {
        // Given
        IndividualWriteDto individual1 = createTestIndividualDto(
            "user1@example.com",
            "User",
            "One",
            "U1123456",
            "+3333333333"
        );

        IndividualWriteDto individual2 = createTestIndividualDto(
            "user2@example.com",
            "User",
            "Two",
            "U2123456",
            "+4444444444"
        );

        registerIndividual(individual1);
        registerIndividual(individual2);

        // When & Then
        mockMvc.perform(get("/v1/persons")
                        .param("email", "user1@example.com", "user2@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    @DisplayName("Должен вернуть пустой список при поиске по несуществующим email")
    void findAllByEmail_with_nonexistent_emails() throws Exception {
        // Given
        String nonExistentEmail = "nonexistent@example.com";

        // When & Then
        mockMvc.perform(get("/v1/persons")
                        .param("email", nonExistentEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    @Transactional
    @DisplayName("Успешное обновление данных пользователя")
    void individual_update() throws Exception {
        // Given
        IndividualWriteDto originalIndividual = createTestIndividualDto(
            "update.test@example.com",
            "Original",
            "Name",
            "ON123456",
            "+5555555555"
        );

        IndividualWriteResponseDto response = registerIndividual(originalIndividual);

        IndividualWriteDto updatedIndividual = createTestIndividualDto(
            "update.test@example.com",
            "Updated",
            "NewName",
            "ON123456",
            "+5555555555"
        );

        // When
        mockMvc.perform(put("/v1/persons/{id}", response.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedIndividual)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.getId()));

        IndividualDto individualFromDB = findIndividual(response.getId());

        // Then
        Assertions.assertNotNull(individualFromDB);
        Assertions.assertEquals(updatedIndividual.getFirstName(), individualFromDB.getFirstName());
        Assertions.assertEquals(updatedIndividual.getLastName(), individualFromDB.getLastName());
    }

    @Test
    @DisplayName("Должен вернуть 404 при обновлении несуществующего пользователя")
    void individual_update_with_invalid_id() throws Exception {
        // Given
        String nonExistentId = "00000000-0000-0000-0000-000000000000";

        IndividualWriteDto updatedIndividual = createTestIndividualDto(
            "update.nonexistent@example.com",
            "NonExistent",
            "User",
            "NE123456",
            "+6666666666"
        );

        // When & Then
        mockMvc.perform(put("/v1/persons/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedIndividual)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Soft удаление пользователя")
    void positive_soft_deletion() throws Exception {
        // Given
        IndividualWriteDto individualDto = createTestIndividualDto(
            "to.delete@example.com",
            "Delete",
            "Me",
            "EF345678",
            "+1111111111"
        );

        IndividualWriteResponseDto response = registerIndividual(individualDto);

        // When
        mockMvc.perform(delete("/v1/persons/{id}", response.getId()))
                .andDo(print())
                .andExpect(status().isOk());

        IndividualDto individual = findIndividual(response.getId());

        // Then
        Assertions.assertNotNull(individual);
        Assertions.assertFalse(individual.getActive());
    }

    @Test
    @Transactional
    @DisplayName("Компенсационный запрос создания пользователя")
    void individual_registration_compensation() throws Exception {
        // Given
        IndividualWriteDto givenIndividual = createTestIndividualDto(
                "compensation.test@example.com",
                "Compensation",
                "Test",
                "CT123456",
                "+7777777777"
        );

        IndividualWriteResponseDto response = registerIndividual(givenIndividual);

        // When
        compensationIndividualRegistration(response.getId());

        // Then
        mockMvc.perform(get("/v1/persons/{id}", response.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Должен вернуть 404 при компенсации регистрации несуществующего пользователя")
    void individual_registration_compensation_with_invalid_id() throws Exception {
        // Given
        String nonExistentId = UUID.randomUUID().toString();

        // When & Then
        mockMvc.perform(delete("/v1/persons/compensate-registration/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    private IndividualWriteDto createTestIndividualDto(String email, String firstName, String lastName, String passportNumber, String phoneNumber) {
        IndividualWriteDto dto = new IndividualWriteDto();
        dto.setEmail(email);
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setPassportNumber(passportNumber);
        dto.setPhoneNumber(phoneNumber);

        AddressWriteDto addressDto = new AddressWriteDto();
        addressDto.setCountryCode("AFG");
        addressDto.setAddress("123 Test Street");
        addressDto.setZipCode("12345");
        addressDto.setCity("Test City");

        dto.setAddress(addressDto);

        return dto;
    }

    private IndividualWriteResponseDto registerIndividual(IndividualWriteDto individual) throws Exception {
        String individualId = mockMvc.perform(post("/v1/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(individual)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(individualId,IndividualWriteResponseDto.class);
    }

    private void compensationIndividualRegistration(String id) throws Exception {
        mockMvc.perform(delete("/v1/persons/compensate-registration/{id}",id))
                .andExpect(status().isOk());
    }

    private IndividualDto findIndividual(String id) throws Exception {
        String response = mockMvc.perform(get("/v1/persons/{id}",id))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response,IndividualDto.class);
    }
}

