package com.example.workshopbooking.workshop.controller;

import com.example.workshopbooking.auth.security.AccountPrincipal;
import com.example.workshopbooking.registration.dto.WorkshopParticipantsResponse;
import com.example.workshopbooking.workshop.dto.InstructorWorkshopListResponse;
import com.example.workshopbooking.workshop.service.InstructorWorkshopService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/instructor/workshops")
public class InstructorWorkshopController {

    private final InstructorWorkshopService instructorWorkshopService;

    public InstructorWorkshopController(InstructorWorkshopService instructorWorkshopService) {
        this.instructorWorkshopService = instructorWorkshopService;
    }

    @GetMapping
    public InstructorWorkshopListResponse listAssignedWorkshops(
            @AuthenticationPrincipal AccountPrincipal instructor) {
        return this.instructorWorkshopService.findAssignedWorkshops(instructor);
    }

    @GetMapping("/{workshopId}/registrations")
    public WorkshopParticipantsResponse listParticipants(
            @PathVariable Long workshopId,
            @AuthenticationPrincipal AccountPrincipal instructor) {
        return this.instructorWorkshopService.findParticipants(workshopId, instructor);
    }
}
