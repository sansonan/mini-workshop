package com.example.workshopbooking.workshop.entity;

import com.example.workshopbooking.auth.entity.Account;
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
@Table(name = "workshops")
public class Workshop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workshop_id")
    private Long workshopId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, length = 5000)
    private String description;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "location", nullable = false, length = 300)
    private String location;

    @Column(name = "max_participants", nullable = false)
    private int maxParticipants;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id", nullable = false)
    private Account instructor;

    protected Workshop() {
    }

    public Workshop(
            String title, String description, Instant startsAt, Instant endsAt,
            String location, int maxParticipants, Account instructor) {
        this.title = title;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.location = location;
        this.maxParticipants = maxParticipants;
        this.instructor = instructor;
    }

    public Long getWorkshopId() { return workshopId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public String getLocation() { return location; }
    public int getMaxParticipants() { return maxParticipants; }
    public Account getInstructor() { return instructor; }
}
