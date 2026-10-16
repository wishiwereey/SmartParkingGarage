package SubsystemA;
public class ParkingEntry {
    private final String plateNumber;
    private String status = "NEW";
    public ParkingEntry(String plateNumber) {
        if (plateNumber == null || plateNumber.isBlank()) {
            throw new IllegalArgumentException("Invalid plate");
        }
        this.plateNumber = plateNumber;
    }
    public void confirmEntry() {
        if (!"NEW".equals(status)) {
            throw new IllegalStateException("Cannot confirm entry");
        }
        status = "ENTERED";
    }
    public void cancelEntry() {
        if (!"ENTERED".equals(status)) {
            throw new IllegalStateException("Cannot cancel entry");
        }
        status = "CANCELLED";
    }
    public String getPlateNumber() {
        return plateNumber;
    }
    public String getStatus() {
        return status;
    }
}
