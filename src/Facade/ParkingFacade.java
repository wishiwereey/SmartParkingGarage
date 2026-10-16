package Facade;
import Persistence.OperationRepository;
import Persistence.EntryRepository;
import SubsystemA.*;
import SubsystemB.*;
import SubsystemC.*;
public class ParkingFacade {
    private final OperationRepository operations = new OperationRepository();
    private final EntryRepository entries = new EntryRepository();
    private final Gate gate = new Gate();
    private final LicensePlateReader reader = new LicensePlateReader();
    private final ReservationManager reservations = new ReservationManager(new SpotRepository());
    private final PaymentTerminal terminal = new PaymentTerminal(new PaymentRepository(), new PaymentValidator());
    private final ParkingAdminFacade admin = new ParkingAdminFacade();
    public int parkCar(String plate, int spot, double amount, boolean failB, boolean failC) {
        reader.scanPlate(plate);
        new PaymentValidator().validate(amount);
        if (spot <= 0) {
            throw new IllegalArgumentException("Invalid spot");
        }
        int id = operations.create(plate, spot, amount);
        try {
            ParkingEntry entry = new ParkingEntry(plate);
            entry.confirmEntry();
            entries.saveForOperation(id, entry);
            gate.openGate();
            gate.closeGate();
            operations.updateStatus(id, "A_DONE");
            if (failB) {
                throw new IllegalStateException("Simulated B failure");
            }
            reservations.reserveSpot(id, spot);
            operations.updateStatus(id, "B_DONE");
            terminal.processPayment(id, plate, amount, failC);
            operations.updateStatus(id, "COMPLETED");
            return id;
        } catch (RuntimeException failure) {
            try {
                admin.compensateOperation(id);
            } catch (RuntimeException compensation) {
                failure.addSuppressed(compensation);
            }
            throw new IllegalStateException("Operation " + id + " failed; status=" + operations.getStatus(id), failure);
        }
    }
}
