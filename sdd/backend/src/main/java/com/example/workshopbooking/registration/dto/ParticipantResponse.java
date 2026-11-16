package com.example.workshopbooking.registration.dto;

import java.time.OffsetDateTime;

public record ParticipantResponse(
        Long registrationId,
        String fullName,
        String email,
        String phoneNumber,
        OffsetDateTime registeredAt) {
}
