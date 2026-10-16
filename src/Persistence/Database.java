package Persistence;
import java.sql.*;
public final class Database {
    private static String url = "jdbc:sqlite:parking.db";
    private Database() {}
    public static void setUrl(String value) {
        if (value == null || !value.startsWith("jdbc:sqlite:")) {
            throw new IllegalArgumentException("Invalid database URL");
        }
        url = value;
    }
    public static Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout=5000");
        }
        return connection;
    }
    public static void initialize() {
        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS parking_operations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        plate_number TEXT NOT NULL,
                        spot_number INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        status TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        plate_number TEXT NOT NULL,
                        status TEXT NOT NULL,
                        operation_id INTEGER
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS parking_spots (
                        spot_number INTEGER PRIMARY KEY,
                        status TEXT NOT NULL,
                        operation_id INTEGER
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS payments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        plate_number TEXT NOT NULL,
                        amount REAL NOT NULL,
                        status TEXT NOT NULL,
                        operation_id INTEGER
                    )
                    """);
            addColumn(connection, "entries", "operation_id");
            addColumn(connection, "parking_spots", "operation_id");
            addColumn(connection, "payments", "operation_id");
            statement.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_entries_operation
                    ON entries(operation_id)
                    WHERE operation_id IS NOT NULL
                    """);
            statement.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_payments_operation
                    ON payments(operation_id)
                    WHERE operation_id IS NOT NULL
                    """);
            statement.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_spots_operation
                    ON parking_spots(operation_id)
                    WHERE operation_id IS NOT NULL
                    """);
        } catch (SQLException e) {
            throw new IllegalStateException("Database initialization failed", e);
        }
    }
    private static void addColumn(Connection connection, String table, String column)
            throws SQLException {
        boolean exists = false;
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "PRAGMA table_info(" + table + ")")) {
            while (result.next()) {
                if (column.equals(result.getString("name"))) {
                    exists = true;
                }
            }
        }
        if (!exists) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(
                        "ALTER TABLE " + table +
                                " ADD COLUMN " + column + " INTEGER");
            }
        }
    }
}
