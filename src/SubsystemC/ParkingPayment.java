package SubsystemC;
public class ParkingPayment {
    private final String plateNumber;
    private final double amount;
    private String status = "PENDING";
    public ParkingPayment(String plate, double amount) {
        if (plate == null ||
                plate.isBlank() ||
                !Double.isFinite(amount) ||
                amount <= 0) {
            throw new IllegalArgumentException("Invalid payment");
        }
        plateNumber = plate;
        this.amount = amount;
    }
    public void pay() {
        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("Cannot pay");
        }
        status = "PAID";
    }
    public void cancel() {
        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("Cannot cancel payment");
        }
        status = "CANCELLED";
    }
    public String getPlateNumber() {
        return plateNumber;
    }
    public double getAmount() {
        return amount;
    }
    public String getStatus() {
        return status;
    }
}