package com.example.workshopbooking;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class DatabaseMigrationIntegrationTests {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("workshop_booking_test");

    @Test
    void appliesAndValidatesTheApprovedSchema() throws SQLException {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();

        assertEquals(1, flyway.migrate().migrationsExecuted);
        flyway.validate();

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            assertEquals(
                    Set.of("accounts", "workshops", "registrations", "email_outbox"),
                    applicationTables(connection));
            assertPrimaryKeyCount(connection);
            assertRestrictiveForeignKeys(connection);
            assertApprovedUniqueConstraints(connection);
            assertApprovedCheckConstraints(connection);
            assertApprovedIndexes(connection);
            assertDerivedStatusIsNotStored(connection);
            assertSuccessfulFlywayHistoryEntry(connection);
        }
    }

    private Set<String> applicationTables(Connection connection) throws SQLException {
        Set<String> tables = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT table_name
                     FROM information_schema.tables
                     WHERE table_schema = 'public'
                       AND table_type = 'BASE TABLE'
                       AND table_name <> 'flyway_schema_history'
                     """)) {
            while (resultSet.next()) {
                tables.add(resultSet.getString("table_name"));
            }
        }
        return tables;
    }

    private void assertPrimaryKeyCount(Connection connection) throws SQLException {
        assertEquals(4, queryLong(connection, """
                SELECT count(*)
                FROM pg_constraint
                WHERE contype = 'p'
                  AND conrelid IN (
                    'public.accounts'::regclass,
                    'public.workshops'::regclass,
                    'public.registrations'::regclass,
                    'public.email_outbox'::regclass
                  )
                """));
    }

    private void assertRestrictiveForeignKeys(Connection connection) throws SQLException {
        assertEquals(3, queryLong(connection, """
                SELECT count(*)
                FROM pg_constraint
                WHERE contype = 'f'
                  AND connamespace = 'public'::regnamespace
                  AND confdeltype = 'r'
                """));
    }

    private void assertApprovedUniqueConstraints(Connection connection) throws SQLException {
        assertTrue(constraintExists(connection, "accounts", "u", "UNIQUE (email)"));
        assertTrue(constraintExists(
                connection, "registrations", "u", "UNIQUE (workshop_id, email_normalized)"));
        assertTrue(constraintExists(connection, "email_outbox", "u", "UNIQUE (registration_id)"));
    }

    private void assertApprovedCheckConstraints(Connection connection) throws SQLException {
        assertEquals(4, checkConstraintCount(connection, "accounts"));
        assertEquals(5, checkConstraintCount(connection, "workshops"));
        assertEquals(3, checkConstraintCount(connection, "registrations"));
        assertEquals(6, checkConstraintCount(connection, "email_outbox"));
    }

    private long checkConstraintCount(Connection connection, String table) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT count(*)
                FROM pg_constraint
                WHERE conrelid = to_regclass(?)
                  AND contype = 'c'
                """)) {
            statement.setString(1, "public." + table);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getLong(1);
            }
        }
    }

    private boolean constraintExists(Connection connection, String table, String kind, String definition)
            throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT EXISTS (
                    SELECT 1
                    FROM pg_constraint
                    WHERE conrelid = to_regclass(?)
                      AND contype = ?
                      AND pg_get_constraintdef(oid) = ?
                )
                """)) {
            statement.setString(1, "public." + table);
            statement.setString(2, kind);
            statement.setString(3, definition);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getBoolean(1);
            }
        }
    }

    private void assertApprovedIndexes(Connection connection) throws SQLException {
        assertEquals(5, queryLong(connection, """
                SELECT count(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname IN (
                    'ix_workshops_instructor_id',
                    'ix_workshops_starts_at',
                    'uq_registrations_workshop_email',
                    'ix_registrations_workshop_registered_at',
                    'ix_email_outbox_pending_next_attempt_at'
                  )
                """));
    }

    private void assertDerivedStatusIsNotStored(Connection connection) throws SQLException {
        assertEquals(0, queryLong(connection, """
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'workshops'
                  AND column_name = 'status'
                """));
    }

    private void assertSuccessfulFlywayHistoryEntry(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT version, success, checksum
                     FROM flyway_schema_history
                     WHERE version = '1'
                     """)) {
            assertTrue(resultSet.next());
            assertEquals("1", resultSet.getString("version"));
            assertTrue(resultSet.getBoolean("success"));
            assertNotNull(resultSet.getObject("checksum"));
            assertFalse(resultSet.next());
        }
    }

    private long queryLong(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getLong(1);
        }
    }
}
