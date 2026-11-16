package com.example.workshopbooking.workshop.dto;

import java.time.OffsetDateTime;

public record WorkshopResponse(
        Long workshopId,
        String title,
        String description,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String location,
        InstructorSummaryResponse instructor,
        int maxParticipants,
        long availableSeats,
        String status) {
}
