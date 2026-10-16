package SubsystemB;
public class ReservationManager {
    private final SpotRepository repository;
    public ReservationManager(SpotRepository repository) {
        this.repository = repository;
    }
    public void reserveSpot(int operationId, int number) {
        repository.reserveForOperation(operationId, number);
    }
    public void releaseSpot(int operationId) {
        repository.releaseForOperation(operationId);
    }
    public boolean isAvailable(int number) {
        ParkingSpot spot = repository.findByNumber(number);
        return spot != null && "AVAILABLE".equals(spot.getStatus());
    }
}
