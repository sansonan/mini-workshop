package com.example.workshopbooking.auth.repository;

import com.example.workshopbooking.auth.entity.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByEmail(String normalizedEmail);
}
