package com.example.workshopbooking.workshop.service;

import com.example.workshopbooking.auth.security.AccountPrincipal;
import com.example.workshopbooking.common.ResourceNotFoundException;
import com.example.workshopbooking.registration.dto.ParticipantResponse;
import com.example.workshopbooking.registration.dto.WorkshopParticipantsResponse;
import com.example.workshopbooking.registration.entity.Registration;
import com.example.workshopbooking.registration.repository.RegistrationRepository;
import com.example.workshopbooking.registration.repository.WorkshopRegistrationCount;
import com.example.workshopbooking.workshop.dto.InstructorSummaryResponse;
import com.example.workshopbooking.workshop.dto.InstructorWorkshopListResponse;
import com.example.workshopbooking.workshop.dto.WorkshopResponse;
import com.example.workshopbooking.workshop.entity.Workshop;
import com.example.workshopbooking.workshop.repository.WorkshopRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstructorWorkshopService {

    private final WorkshopRepository workshopRepository;
    private final RegistrationRepository registrationRepository;
    private final ZoneId trainingCenterZone;

    public InstructorWorkshopService(
            WorkshopRepository workshopRepository,
            RegistrationRepository registrationRepository,
            @Value("${workshop.time-zone:Asia/Phnom_Penh}") String trainingCenterTimeZone) {
        this.workshopRepository = workshopRepository;
        this.registrationRepository = registrationRepository;
        this.trainingCenterZone = ZoneId.of(trainingCenterTimeZone);
    }

    @Transactional(readOnly = true)
    public InstructorWorkshopListResponse findAssignedWorkshops(AccountPrincipal instructor) {
        Long accountId = instructor.getAccountId();
        List<Workshop> workshops = this.workshopRepository
                .findAllByInstructor_AccountIdOrderByStartsAtAsc(accountId);
        List<Long> workshopIds = workshops.stream().map(Workshop::getWorkshopId).toList();
        Map<Long, Long> registrationCounts = registrationCounts(workshopIds);
        ZonedDateTime nowAtTrainingCenter = ZonedDateTime.now(this.trainingCenterZone);

        List<WorkshopResponse> results = workshops.stream()
                .map(workshop -> workshopResponse(
                        workshop,
                        registrationCounts.getOrDefault(workshop.getWorkshopId(), 0L),
                        nowAtTrainingCenter))
                .toList();
        return new InstructorWorkshopListResponse(results);
    }

    @Transactional(readOnly = true)
    public WorkshopParticipantsResponse findParticipants(Long workshopId, AccountPrincipal instructor) {
        Workshop workshop = this.workshopRepository.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop was not found."));

        if (!Objects.equals(workshop.getInstructor().getAccountId(), instructor.getAccountId())) {
            throw new AccessDeniedException("Instructor is not assigned to this workshop.");
        }

        List<ParticipantResponse> participants = this.registrationRepository
                .findAllByWorkshop_WorkshopIdOrderByRegisteredAtAsc(workshopId)
                .stream()
                .map(this::participantResponse)
                .toList();
        return new WorkshopParticipantsResponse(workshopId, participants);
    }

    private Map<Long, Long> registrationCounts(List<Long> workshopIds) {
        if (workshopIds.isEmpty()) {
            return Map.of();
        }
        return this.registrationRepository.countForWorkshopIds(workshopIds).stream()
                .collect(Collectors.toMap(WorkshopRegistrationCount::getWorkshopId,
                        WorkshopRegistrationCount::getRegistrationCount));
    }

    private WorkshopResponse workshopResponse(
            Workshop workshop, long registrationCount, ZonedDateTime nowAtTrainingCenter) {
        boolean ended = !nowAtTrainingCenter.isBefore(workshop.getEndsAt().atZone(this.trainingCenterZone));
        String status = ended
                ? "COMPLETED"
                : registrationCount >= workshop.getMaxParticipants() ? "FULL" : "OPEN";

        return new WorkshopResponse(
                workshop.getWorkshopId(),
                workshop.getTitle(),
                workshop.getDescription(),
                OffsetDateTime.ofInstant(workshop.getStartsAt(), this.trainingCenterZone),
                OffsetDateTime.ofInstant(workshop.getEndsAt(), this.trainingCenterZone),
                workshop.getLocation(),
                new InstructorSummaryResponse(workshop.getInstructor().getDisplayName()),
                workshop.getMaxParticipants(),
                workshop.getMaxParticipants() - registrationCount,
                status);
    }

    private ParticipantResponse participantResponse(Registration registration) {
        Instant registeredAt = registration.getRegisteredAt();
        return new ParticipantResponse(
                registration.getRegistrationId(),
                registration.getParticipantName(),
                registration.getEmailNormalized(),
                registration.getPhoneNumber(),
                OffsetDateTime.ofInstant(registeredAt, this.trainingCenterZone));
    }
}
