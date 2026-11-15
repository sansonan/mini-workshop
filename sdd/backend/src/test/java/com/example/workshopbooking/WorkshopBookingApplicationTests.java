package com.example.workshopbooking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkshopBookingApplicationTests {

    @Test
    void applicationEntryPointIsMarkedForSpringBoot() {
        assertTrue(WorkshopBookingApplication.class.isAnnotationPresent(SpringBootApplication.class));
    }
}
