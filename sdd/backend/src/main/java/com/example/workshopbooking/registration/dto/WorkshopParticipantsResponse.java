package com.example.workshopbooking.registration.dto;

import java.util.List;

public record WorkshopParticipantsResponse(Long workshopId, List<ParticipantResponse> participants) {
}
