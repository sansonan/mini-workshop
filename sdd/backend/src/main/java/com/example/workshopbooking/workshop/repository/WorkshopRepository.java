package com.example.workshopbooking.workshop.repository;

import com.example.workshopbooking.workshop.entity.Workshop;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkshopRepository extends JpaRepository<Workshop, Long> {

    List<Workshop> findAllByInstructor_AccountIdOrderByStartsAtAsc(Long instructorAccountId);
}
