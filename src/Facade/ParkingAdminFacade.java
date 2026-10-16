package Facade;
import Persistence.OperationRepository;
import Persistence.EntryRepository;
import SubsystemA.*;
import SubsystemB.*;
import SubsystemC.*;
public class ParkingAdminFacade {
    private final OperationRepository operations = new OperationRepository();
    private final EntryRepository entries = new EntryRepository();
    private final SpotRepository spots = new SpotRepository();
    private final PaymentRepository payments = new PaymentRepository();
    public String inspectOperation(int id) {
        OperationRepository.Operation operation = operations.get(id);
        ParkingEntry entry = entries.findByOperation(id);
        ParkingSpot spot = spots.findByOperation(id);
        ParkingPayment payment = payments.findByOperation(id);
        return "Operation #" + id +
                " [" + operation.status() + "]" +
                " plate=" + operation.plate() +
                " spot=" + operation.spot() +
                " entry=" + (
                entry == null ? "NONE" : entry.getStatus()
        ) +
                " reservation=" + (
                spot == null ? "NONE" : spot.getStatus()
        ) +
                " payment=" + (
                payment == null ? "NONE" : payment.getStatus());
    }
    public void compensateOperation(int id) {
        String status = operations.getStatus(id);
        if ("COMPENSATED".equals(status)) {
            return;
        }
        if ("COMPLETED".equals(status) ||
                "REFUND_REQUIRED".equals(status)) {
            throw new IllegalStateException("Completed or paid operation cannot be automatically refunded");
        }
        ParkingPayment payment = payments.findByOperation(id);
        if (payment != null && "PAID".equals(payment.getStatus())) {
            operations.updateStatus(id, "REFUND_REQUIRED");
            throw new IllegalStateException("Refund required for operation " + id);
        }
        operations.updateStatus(id, "COMPENSATING");
        try {
            if (payment != null && "PENDING".equals(payment.getStatus())) {
                payment.cancel();
                payments.updateStatusByOperation(id, payment);
            }
            spots.releaseForOperation(id);
            ParkingEntry entry = entries.findByOperation(id);
            if (entry != null && "ENTERED".equals(entry.getStatus())) {
                entry.cancelEntry();
                entries.updateStatusByOperation(id, entry);
            }
            operations.updateStatus(id, "COMPENSATED");
        } catch (RuntimeException e) {
            operations.updateStatus(id, "COMPENSATION_FAILED");
            throw e;
        }
    }
}
