package SubsystemB;
import Persistence.Database;
import java.sql.*;
public class SpotRepository {
    public void save(ParkingSpot spot) {
        String sql = """
                INSERT INTO parking_spots(spot_number, status)
                VALUES (?, ?)
                ON CONFLICT(spot_number) DO NOTHING
                """;
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, spot.getSpotNumber());
            statement.setString(2, spot.getStatus());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot save spot", e);
        }
    }
    public ParkingSpot findByNumber(int number) {
        String sql = """
                SELECT status
                FROM parking_spots
                WHERE spot_number = ?
                """;
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, number);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return hydrate(number, result.getString("status"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load spot", e);
        }
    }
    public ParkingSpot findByOperation(int id) {
        String sql = """
                SELECT spot_number, status
                FROM parking_spots
                WHERE operation_id = ?
                """;
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return hydrate(
                        result.getInt("spot_number"),
                        result.getString("status"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load operation spot", e);
        }
    }
    private ParkingSpot hydrate(int number, String status) {
        ParkingSpot spot = new ParkingSpot(number);
        if ("RESERVED".equals(status) || "OCCUPIED".equals(status)) {
            spot.reserve();
        }
        if ("OCCUPIED".equals(status)) {
            spot.occupy();
        }
        if (!"AVAILABLE".equals(status) && !"RESERVED".equals(status) && !"OCCUPIED".equals(status)) {
            throw new IllegalStateException("Unknown spot status");
        }
        return spot;
    }
    public void reserveForOperation(int id, int number) {
        String sql = """
                UPDATE parking_spots
                SET status = 'RESERVED', operation_id = ?
                WHERE spot_number = ?
                AND status = 'AVAILABLE'
                AND operation_id IS NULL
                """;
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, number);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("Spot unavailable: " + number);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot reserve spot", e);
        }
    }
    public void releaseForOperation(int id) {
        String sql = """
                UPDATE parking_spots
                SET status = 'AVAILABLE', operation_id = NULL
                WHERE operation_id = ?
                AND status IN ('RESERVED', 'OCCUPIED')
                """;
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot release spot", e);
        }
    }
}
