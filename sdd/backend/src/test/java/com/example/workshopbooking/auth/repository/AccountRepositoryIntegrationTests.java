package com.example.workshopbooking.auth.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.workshopbooking.auth.entity.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class AccountRepositoryIntegrationTests {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("workshop_account_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private AccountRepository repository;

    @Test
    void mapsApprovedAccountsTableAndFindsCanonicalEmail() {
        Account saved = this.repository.saveAndFlush(
                new Account("admin@example.com", "$2a$12$examplehash", "Workshop Admin", "ADMIN", true));

        Account loaded = this.repository.findByEmail("admin@example.com").orElseThrow();

        assertTrue(saved.getAccountId() > 0);
        assertEquals(saved.getAccountId(), loaded.getAccountId());
        assertEquals("Workshop Admin", loaded.getDisplayName());
        assertEquals("ADMIN", loaded.getRole());
        assertTrue(loaded.isActive());
    }
}
