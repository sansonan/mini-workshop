package com.example.workshopbooking.workshop.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.workshopbooking.auth.entity.Account;
import com.example.workshopbooking.auth.repository.AccountRepository;
import com.example.workshopbooking.registration.entity.Registration;
import com.example.workshopbooking.registration.repository.RegistrationRepository;
import com.example.workshopbooking.workshop.entity.Workshop;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class InstructorOwnershipIntegrationTests {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("workshop_instructor_ownership_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private WorkshopRepository workshopRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Test
    void repositoryQueriesMapApprovedSchemaAndScopeWorkshopsAndParticipants() {
        Account firstInstructor = accountRepository.saveAndFlush(
                new Account("first@example.com", "hash-1", "First Instructor", "INSTRUCTOR", true));
        Account secondInstructor = accountRepository.saveAndFlush(
                new Account("second@example.com", "hash-2", "Second Instructor", "INSTRUCTOR", true));
        Workshop firstWorkshop = workshopRepository.saveAndFlush(workshop(firstInstructor, "First Workshop"));
        Workshop secondWorkshop = workshopRepository.saveAndFlush(workshop(secondInstructor, "Second Workshop"));
        registrationRepository.saveAndFlush(registration(firstWorkshop, "first participant", "p1@example.com"));
        registrationRepository.saveAndFlush(registration(secondWorkshop, "second participant", "p2@example.com"));

        List<Workshop> assigned = workshopRepository
                .findAllByInstructor_AccountIdOrderByStartsAtAsc(firstInstructor.getAccountId());
        List<Registration> participants = registrationRepository
                .findAllByWorkshop_WorkshopIdOrderByRegisteredAtAsc(firstWorkshop.getWorkshopId());
        var counts = registrationRepository.countForWorkshopIds(
                List.of(firstWorkshop.getWorkshopId(), secondWorkshop.getWorkshopId()));

        assertEquals(List.of(firstWorkshop.getWorkshopId()),
                assigned.stream().map(Workshop::getWorkshopId).toList());
        assertEquals(List.of("first participant"),
                participants.stream().map(Registration::getParticipantName).toList());
        assertEquals(2, counts.size());
        assertTrue(counts.stream().allMatch(count -> count.getRegistrationCount() == 1L));
    }

    private static Workshop workshop(Account instructor, String title) {
        return new Workshop(title, "Workshop description", Instant.parse("2027-06-18T02:00:00Z"),
                Instant.parse("2027-06-18T05:00:00Z"), "Room 2", 16, instructor);
    }

    private static Registration registration(Workshop workshop, String name, String email) {
        return new Registration(workshop, name, email, null, Instant.parse("2027-06-01T07:25:10Z"));
    }
}
