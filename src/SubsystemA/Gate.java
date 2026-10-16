package SubsystemA;
public class Gate {
    private boolean open;
    public void openGate() {
        open = true;
        System.out.println("Gate opened. Car can enter.");
    }
    public void closeGate() {
        open = false;
        System.out.println("Gate closed.");
    }
    public boolean isGateOpen() {
        return open;
    }
}

