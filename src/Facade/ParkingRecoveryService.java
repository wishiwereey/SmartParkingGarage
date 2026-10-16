package Facade;
import Persistence.OperationRepository;
public class ParkingRecoveryService {
    private final OperationRepository operations = new OperationRepository();
    private final ParkingAdminFacade admin = new ParkingAdminFacade();
    public int recover() {
        int recovered = 0;
        for (int id : operations.findUnfinishedOperations()) {
            try {
                admin.compensateOperation(id);
                recovered++;
            } catch (RuntimeException e) {
                System.err.println("Recovery #" + id + ": " + e.getMessage());
            }
        }
        return recovered;
    }
}
