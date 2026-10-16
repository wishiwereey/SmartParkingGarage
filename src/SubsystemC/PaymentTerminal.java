package SubsystemC;
public class PaymentTerminal {
    private final PaymentRepository repository;
    private final PaymentValidator validator;
    public PaymentTerminal(
            PaymentRepository repository,
            PaymentValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }
    public void processPayment(int id,String plate, double amount, boolean fail) {
        validator.validate(amount);
        ParkingPayment payment = new ParkingPayment(plate, amount);
        repository.saveForOperation(id, payment);
        if (fail) {
            payment.cancel();
            repository.updateStatusByOperation(id, payment);
            throw new IllegalStateException("Payment declined");
        }
        payment.pay();
        repository.updateStatusByOperation(id, payment);
    }
}
