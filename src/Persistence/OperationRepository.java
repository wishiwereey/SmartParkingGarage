package Persistence;
import java.sql.*;
import java.util.*;
public class OperationRepository {
    public record Operation(int id, String plate, int spot, double amount, String status) {}
    public int create(String plate, int spot, double amount) {
        String sql = """
                INSERT INTO parking_operations
                (plate_number, spot_number, amount, status)
                VALUES (?, ?, ?, 'NEW')
                """;
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, plate);
            statement.setInt(2, spot);
            statement.setDouble(3, amount);
            statement.executeUpdate();
            try (ResultSet result = statement.getGeneratedKeys()) {
                if (result.next()) {
                    return result.getInt(1);
                }
            }
            throw new IllegalStateException("No ID");
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot create operation", e);
        }
    }
    public Operation get(int id) {
        String sql = "SELECT * FROM parking_operations WHERE id = ?";
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return new Operation(result.getInt("id"), result.getString("plate_number"), result.getInt("spot_number"), result.getDouble("amount"), result.getString("status"));
                }
            }
            throw new IllegalArgumentException("Operation not found: " + id);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load operation", e);
        }
    }
    public String getStatus(int id) {
        return get(id).status();
    }
    public void updateStatus(int id, String status) {
        String sql = """
                UPDATE parking_operations
                SET status = ?
                WHERE id = ?
                """;
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, id);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Operation not found");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot update operation", e);
        }
    }
    public List<Integer> findUnfinishedOperations() {
        List<Integer> ids = new ArrayList<>();
        String sql = """
                SELECT id
                FROM parking_operations
                WHERE status IN (
                    'NEW',
                    'A_DONE',
                    'B_DONE',
                    'COMPENSATING',
                    'COMPENSATION_FAILED'
                )
                ORDER BY id
                """;
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                ids.add(result.getInt(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot list operations", e);
        }
        return ids;
    }
    public int findLatestIdByPlate(String plate) {
        String sql = """
                SELECT id
                FROM parking_operations
                WHERE plate_number = ?
                ORDER BY id DESC
                LIMIT 1
                """;
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, plate);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt(1);
                }
            }
            throw new IllegalArgumentException("Operation not found");
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot find operation", e);
        }
    }
}
