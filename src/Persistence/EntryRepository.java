package Persistence;
import SubsystemA.ParkingEntry;
import java.sql.*;
public class EntryRepository {
    public void saveForOperation(int id, ParkingEntry entry) {
        String sql = """
                INSERT INTO entries(operation_id, plate_number, status)
                VALUES (?, ?, ?)
                """;
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setString(2, entry.getPlateNumber());
            statement.setString(3, entry.getStatus());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot save entry", e);
        }
    }
    public ParkingEntry findByOperation(int id) {
        String sql = """
                SELECT plate_number, status
                FROM entries
                WHERE operation_id = ?
                """;
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                ParkingEntry entry = new ParkingEntry(result.getString("plate_number"));
                String status = result.getString("status");
                if ("ENTERED".equals(status) || "CANCELLED".equals(status)) {
                    entry.confirmEntry();
                }
                if ("CANCELLED".equals(status)) {
                    entry.cancelEntry();
                }
                if (!"NEW".equals(status) && !"ENTERED".equals(status) && !"CANCELLED".equals(status)) {
                    throw new IllegalStateException("Unknown entry status");
                }
                return entry;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load entry", e);
        }
    }
    public void updateStatusByOperation(int id, ParkingEntry entry) {
        String sql = """
                UPDATE entries
                SET status = ?
                WHERE operation_id = ?
                """;
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entry.getStatus());
            statement.setInt(2, id);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("Entry not found");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot update entry", e);
        }
    }
}
