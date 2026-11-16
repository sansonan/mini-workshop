package com.example.workshopbooking.registration.entity;

import com.example.workshopbooking.workshop.entity.Workshop;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "registrations")
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registration_id")
    private Long registrationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workshop_id", nullable = false)
    private Workshop workshop;

    @Column(name = "participant_name", nullable = false, length = 200)
    private String participantName;

    @Column(name = "email_normalized", nullable = false, length = 320)
    private String emailNormalized;

    @Column(name = "phone_number", length = 40)
    private String phoneNumber;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    protected Registration() {
    }

    public Registration(
            Workshop workshop, String participantName, String emailNormalized,
            String phoneNumber, Instant registeredAt) {
        this.workshop = workshop;
        this.participantName = participantName;
        this.emailNormalized = emailNormalized;
        this.phoneNumber = phoneNumber;
        this.registeredAt = registeredAt;
    }

    public Long getRegistrationId() { return registrationId; }
    public Workshop getWorkshop() { return workshop; }
    public String getParticipantName() { return participantName; }
    public String getEmailNormalized() { return emailNormalized; }
    public String getPhoneNumber() { return phoneNumber; }
    public Instant getRegisteredAt() { return registeredAt; }
}
