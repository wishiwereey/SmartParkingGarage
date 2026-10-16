package SubsystemB;
public class ParkingSpot {
    private final int spotNumber;
    private String status = "AVAILABLE";
    public ParkingSpot(int number) {
        if (number <= 0) {
            throw new IllegalArgumentException("Invalid spot");
        }
        spotNumber = number;
    }
    public void reserve() {
        if (!"AVAILABLE".equals(status)) {
            throw new IllegalStateException("Spot not available");
        }
        status = "RESERVED";
    }
    public void occupy() {
        if (!"RESERVED".equals(status)) {
            throw new IllegalStateException("Spot not reserved");
        }
        status = "OCCUPIED";
    }
    public void release() {
        if (!"RESERVED".equals(status) &&
                !"OCCUPIED".equals(status)) {
            throw new IllegalStateException("Spot already available");
        }
        status = "AVAILABLE";
    }
    public int getSpotNumber() {
        return spotNumber;
    }
    public String getStatus() {
        return status;
    }
}
