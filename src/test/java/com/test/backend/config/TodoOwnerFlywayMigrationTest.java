package com.test.backend.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MariaDBContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TodoOwnerFlywayMigrationTest {

    @Test
    void freshDatabaseMigratesWithRequiredTodoOwner() throws Exception {
        String url = dbUrl();

        migrate(url, null, Map.of());

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            assertThat(columnNullable(connection, "todos", "owner_id")).isEqualTo("NO");
        }
    }

    @Test
    void assignsLegacyTodosToOnlyExistingUser() throws Exception {
        String url = dbUrl();
        migrate(url, "1", Map.of());

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            insertUser(connection, "owner@example.com");
            insertLegacyTodo(connection, "legacy-1");
        }

        migrate(url, null, Map.of());

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            assertThat(todoOwnerEmail(connection, "legacy-1")).isEqualTo("owner@example.com");
            assertThat(columnNullable(connection, "todos", "owner_id")).isEqualTo("NO");
        }
    }

    @Test
    void assignsLegacyTodosToConfiguredOwnerWhenMultipleUsersExist() throws Exception {
        String url = dbUrl();
        migrate(url, "1", Map.of());

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            insertUser(connection, "owner@example.com");
            insertUser(connection, "other@example.com");
            insertLegacyTodo(connection, "legacy-1");
        }

        migrate(url, null, Map.of("legacyOwnerEmail", "owner@example.com"));

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            assertThat(todoOwnerEmail(connection, "legacy-1")).isEqualTo("owner@example.com");
        }
    }

    @Test
    void failsWhenLegacyTodosHaveMultiplePossibleOwnersAndNoConfiguredOwner() throws Exception {
        String url = dbUrl();
        migrate(url, "1", Map.of());

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            insertUser(connection, "owner@example.com");
            insertUser(connection, "other@example.com");
            insertLegacyTodo(connection, "legacy-1");
        }

        assertThatThrownBy(() -> migrate(url, null, Map.of()))
                .isInstanceOf(FlywayException.class)
                .satisfies(error -> assertThat(rootCause(error).getMessage())
                        .contains("TODO_LEGACY_OWNER_EMAIL"));
    }

    @Test
    void failsWhenConfiguredOwnerDoesNotExist() throws Exception {
        String url = dbUrl();
        migrate(url, "1", Map.of());

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            insertUser(connection, "owner@example.com");
            insertLegacyTodo(connection, "legacy-1");
        }

        assertThatThrownBy(() -> migrate(url, null, Map.of("legacyOwnerEmail", "missing@example.com")))
                .isInstanceOf(FlywayException.class)
                .satisfies(error -> assertThat(rootCause(error).getMessage())
                        .contains("does not match an existing user"));
    }

    @Test
    void mariaDbMigrationBackfillsOwnerAndRequiresOwnerColumn() throws Exception {
        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Docker is required for the MariaDB migration integration test."
        );

        try (MariaDBContainer<?> maria = new MariaDBContainer<>("mariadb:11.4")) {
            maria.start();
            migrate(maria.getJdbcUrl(), "1", Map.of(), maria.getUsername(), maria.getPassword());

            try (Connection connection = DriverManager.getConnection(
                    maria.getJdbcUrl(),
                    maria.getUsername(),
                    maria.getPassword()
            )) {
                insertUser(connection, "owner@example.com");
                insertUser(connection, "other@example.com");
                insertLegacyTodo(connection, "legacy-1");
            }

            migrate(
                    maria.getJdbcUrl(),
                    null,
                    Map.of("legacyOwnerEmail", "owner@example.com"),
                    maria.getUsername(),
                    maria.getPassword()
            );

            try (Connection connection = DriverManager.getConnection(
                    maria.getJdbcUrl(),
                    maria.getUsername(),
                    maria.getPassword()
            )) {
                assertThat(todoOwnerEmail(connection, "legacy-1")).isEqualTo("owner@example.com");
                assertThat(columnNullable(connection, "todos", "owner_id")).isEqualTo("NO");
                assertThat(foreignKeyExists(connection, "todos", "fk_todos_owner")).isTrue();
                assertThat(indexExists(connection, "todos", "idx_todos_owner")).isTrue();
            }
        }
    }

    private String dbUrl() {
        return "jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1";
    }

    private void migrate(String url, String target, Map<String, String> placeholders) {
        migrate(url, target, placeholders, "sa", "");
    }

    private void migrate(
            String url,
            String target,
            Map<String, String> placeholders,
            String username,
            String password
    ) {
        var configuration = Flyway.configure()
                .dataSource(url, username, password)
                .locations("classpath:db/migration")
                .placeholders(placeholders);
        if (target != null) {
            configuration.target(target);
        }
        configuration.load().migrate();
    }

    private void insertUser(Connection connection, String email) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO users (email, password, name, role)
                    VALUES ('%s', 'password', 'User', 'USER')
                    """.formatted(email));
        }
    }

    private void insertLegacyTodo(Connection connection, String id) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO todos (id, title, completed, priority, recurrence, created_at, updated_at)
                    VALUES ('%s', 'Legacy todo', false, 'MEDIUM', 'NONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """.formatted(id));
        }
    }

    private String todoOwnerEmail(Connection connection, String todoId) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("""
                     SELECT u.email
                     FROM todos t
                     JOIN users u ON u.id = t.owner_id
                     WHERE t.id = '%s'
                     """.formatted(todoId))) {
            assertThat(rs.next()).isTrue();
            return rs.getString(1);
        }
    }

    private boolean foreignKeyExists(Connection connection, String tableName, String foreignKeyName) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getImportedKeys(null, null, tableName)) {
            while (rs.next()) {
                if (foreignKeyName.equalsIgnoreCase(rs.getString("FK_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getIndexInfo(null, null, tableName, false, false)) {
            while (rs.next()) {
                if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String columnNullable(Connection connection, String tableName, String columnName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("""
                     SELECT is_nullable
                     FROM information_schema.columns
                     WHERE table_name = '%s' AND column_name = '%s'
                     """.formatted(tableName, columnName))) {
            assertThat(rs.next()).isTrue();
            return rs.getString(1);
        }
    }

    private Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
