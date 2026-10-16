package SubsystemC;
public class PaymentValidator {
    public void validate(double amount) {
        if (!Double.isFinite(amount) ||
                amount <= 0 ||
                amount > 100000) {
            throw new IllegalArgumentException("Invalid payment amount");
        }
    }
}