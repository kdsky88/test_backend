package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public class V2__todo_owner_backfill_and_require_owner extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String product = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        Dialect dialect = product.contains("h2") ? Dialect.H2 : Dialect.MARIA;

        if (!columnExists(connection, "todos", "owner_id")) {
            execute(connection, "ALTER TABLE todos ADD COLUMN owner_id BIGINT");
        }

        long unownedCount = count(connection, "SELECT COUNT(*) FROM todos WHERE owner_id IS NULL");
        if (unownedCount > 0) {
            long ownerId = resolveLegacyOwnerId(connection, context.getConfiguration().getPlaceholders().get("legacyOwnerEmail"), unownedCount);
            try (PreparedStatement statement = connection.prepareStatement("UPDATE todos SET owner_id = ? WHERE owner_id IS NULL")) {
                statement.setLong(1, ownerId);
                statement.executeUpdate();
            }
        }

        if (count(connection, "SELECT COUNT(*) FROM todos WHERE owner_id IS NULL") > 0) {
            throw new IllegalStateException("Cannot make todos.owner_id mandatory while unowned todos remain.");
        }

        if (!indexExists(connection, "todos", "idx_todos_owner")) {
            execute(connection, "CREATE INDEX idx_todos_owner ON todos (owner_id)");
        }
        if (!foreignKeyExists(connection, "todos", "fk_todos_owner")) {
            execute(connection, "ALTER TABLE todos ADD CONSTRAINT fk_todos_owner FOREIGN KEY (owner_id) REFERENCES users (id)");
        }

        makeOwnerIdNotNull(connection, dialect);
    }

    private long resolveLegacyOwnerId(Connection connection, String legacyOwnerEmail, long unownedCount) throws SQLException {
        String email = legacyOwnerEmail == null ? "" : legacyOwnerEmail.trim();
        if (!email.isEmpty()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM users WHERE email = ?")) {
                statement.setString(1, email);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
            throw new IllegalStateException("todo.legacy-owner-email does not match an existing user: " + email);
        }

        long userCount = count(connection, "SELECT COUNT(*) FROM users");
        if (userCount == 1) {
            return count(connection, "SELECT id FROM users");
        }

        throw new IllegalStateException(
                "Found " + unownedCount + " todos without owner. Set TODO_LEGACY_OWNER_EMAIL to an existing user email before startup.");
    }

    private void makeOwnerIdNotNull(Connection connection, Dialect dialect) throws SQLException {
        if (dialect == Dialect.H2) {
            execute(connection, "ALTER TABLE todos ALTER COLUMN owner_id SET NOT NULL");
        } else {
            execute(connection, "ALTER TABLE todos MODIFY owner_id BIGINT NOT NULL");
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = metaData.getColumns(null, null, tableName.toUpperCase(Locale.ROOT), columnName.toUpperCase(Locale.ROOT))) {
            return rs.next();
        }
    }

    private boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getIndexInfo(null, null, tableName, false, false)) {
            while (rs.next()) {
                if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) return true;
            }
        }
        try (ResultSet rs = metaData.getIndexInfo(null, null, tableName.toUpperCase(Locale.ROOT), false, false)) {
            while (rs.next()) {
                if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) return true;
            }
        }
        return false;
    }

    private boolean foreignKeyExists(Connection connection, String tableName, String fkName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getImportedKeys(null, null, tableName)) {
            while (rs.next()) {
                if (fkName.equalsIgnoreCase(rs.getString("FK_NAME"))) return true;
            }
        }
        try (ResultSet rs = metaData.getImportedKeys(null, null, tableName.toUpperCase(Locale.ROOT))) {
            while (rs.next()) {
                if (fkName.equalsIgnoreCase(rs.getString("FK_NAME"))) return true;
            }
        }
        return false;
    }

    private long count(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            if (!rs.next()) {
                throw new SQLException("Query returned no rows: " + sql);
            }
            return rs.getLong(1);
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private enum Dialect {
        H2, MARIA
    }
}
