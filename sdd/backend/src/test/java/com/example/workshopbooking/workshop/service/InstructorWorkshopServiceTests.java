package com.example.workshopbooking.workshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.workshopbooking.auth.entity.Account;
import com.example.workshopbooking.auth.security.AccountPrincipal;
import com.example.workshopbooking.common.ResourceNotFoundException;
import com.example.workshopbooking.registration.entity.Registration;
import com.example.workshopbooking.registration.repository.RegistrationRepository;
import com.example.workshopbooking.workshop.entity.Workshop;
import com.example.workshopbooking.workshop.repository.WorkshopRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InstructorWorkshopServiceTests {

    @Mock
    private WorkshopRepository workshopRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    private InstructorWorkshopService service;
    private AccountPrincipal instructor;

    @BeforeEach
    void setUp() {
        this.service = new InstructorWorkshopService(
                this.workshopRepository, this.registrationRepository, "Asia/Phnom_Penh");
        this.instructor = principal(31L, "instructor@example.com");
    }

    @Test
    void instructorWorkshopListUsesOnlyAuthenticatedAccountId() {
        Workshop assigned = workshop(101L, account(31L, "instructor@example.com"));
        when(this.workshopRepository.findAllByInstructor_AccountIdOrderByStartsAtAsc(31L))
                .thenReturn(List.of(assigned));

        var response = this.service.findAssignedWorkshops(this.instructor);

        assertEquals(List.of(101L), response.workshops().stream().map(item -> item.workshopId()).toList());
        verify(this.workshopRepository).findAllByInstructor_AccountIdOrderByStartsAtAsc(31L);
        verify(this.registrationRepository).countForWorkshopIds(List.of(101L));
    }

    @Test
    void assignedInstructorCanRetrieveWorkshopParticipants() {
        Workshop assigned = workshop(101L, account(31L, "instructor@example.com"));
        Registration registration = new Registration(
                assigned, "Dara Vann", "dara@example.com", "+855 12 345 678", Instant.parse("2027-06-01T07:25:10Z"));
        when(this.workshopRepository.findById(101L)).thenReturn(Optional.of(assigned));
        when(this.registrationRepository.findAllByWorkshop_WorkshopIdOrderByRegisteredAtAsc(101L))
                .thenReturn(List.of(registration));

        var response = this.service.findParticipants(101L, this.instructor);

        assertEquals(101L, response.workshopId());
        assertEquals("Dara Vann", response.participants().getFirst().fullName());
        assertEquals("dara@example.com", response.participants().getFirst().email());
        verify(this.registrationRepository).findAllByWorkshop_WorkshopIdOrderByRegisteredAtAsc(101L);
    }

    @Test
    void otherInstructorsParticipantsAreForbiddenBeforeParticipantQuery() {
        Workshop assignedToSomeoneElse = workshop(202L, account(32L, "other@example.com"));
        when(this.workshopRepository.findById(202L)).thenReturn(Optional.of(assignedToSomeoneElse));

        assertThrows(AccessDeniedException.class,
                () -> this.service.findParticipants(202L, this.instructor));

        verify(this.registrationRepository, never()).findAllByWorkshop_WorkshopIdOrderByRegisteredAtAsc(202L);
    }

    @Test
    void nonexistentWorkshopReturnsNotFoundWithoutQueryingParticipants() {
        when(this.workshopRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> this.service.findParticipants(999L, this.instructor));

        verifyNoInteractions(this.registrationRepository);
    }

    private static Workshop workshop(Long id, Account instructor) {
        Workshop workshop = new Workshop(
                "Intro to Woodworking", "Beginner workshop", Instant.parse("2027-06-18T02:00:00Z"),
                Instant.parse("2027-06-18T05:00:00Z"), "Room 2", 16, instructor);
        ReflectionTestUtils.setField(workshop, "workshopId", id);
        return workshop;
    }

    private static AccountPrincipal principal(Long id, String email) {
        return new AccountPrincipal(account(id, email));
    }

    private static Account account(Long id, String email) {
        Account account = new Account(email, "test-hash", "Instructor Name", "INSTRUCTOR", true);
        ReflectionTestUtils.setField(account, "accountId", id);
        return account;
    }
}
