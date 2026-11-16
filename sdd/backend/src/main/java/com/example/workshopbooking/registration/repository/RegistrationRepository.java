package com.example.workshopbooking.registration.repository;

import com.example.workshopbooking.registration.entity.Registration;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    List<Registration> findAllByWorkshop_WorkshopIdOrderByRegisteredAtAsc(Long workshopId);

    @Query("""
            select r.workshop.workshopId as workshopId, count(r) as registrationCount
            from Registration r
            where r.workshop.workshopId in :workshopIds
            group by r.workshop.workshopId
            """)
    List<WorkshopRegistrationCount> countForWorkshopIds(@Param("workshopIds") Collection<Long> workshopIds);
}
