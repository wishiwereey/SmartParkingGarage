package SubsystemC;
import Persistence.Database;
import java.sql.*;
public class PaymentRepository {
    public void saveForOperation(int id, ParkingPayment payment) {
        String sql = """
                INSERT INTO payments
                (operation_id, plate_number, amount, status)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setString(2, payment.getPlateNumber());
            statement.setDouble(3, payment.getAmount());
            statement.setString(4, payment.getStatus());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot save payment", e);
        }
    }
    public ParkingPayment findByOperation(int id) {
        String sql = """
                SELECT plate_number, amount, status
                FROM payments
                WHERE operation_id = ?
                """;
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                ParkingPayment payment = new ParkingPayment(
                        result.getString("plate_number"),
                        result.getDouble("amount"));
                String status = result.getString("status");
                if ("PAID".equals(status)) {
                    payment.pay();
                } else if ("CANCELLED".equals(status)) {
                    payment.cancel();
                } else if (!"PENDING".equals(status)) {
                    throw new IllegalStateException("Unknown payment status");
                }
                return payment;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load payment", e);
        }
    }
    public void updateStatusByOperation(
            int id,
            ParkingPayment payment) {
        String sql = """
                UPDATE payments
                SET status = ?
                WHERE operation_id = ?
                """;
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, payment.getStatus());
            statement.setInt(2, id);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("Payment not found");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot update payment", e);
        }
    }
}
