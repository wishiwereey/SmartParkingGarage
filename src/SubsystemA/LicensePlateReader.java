package SubsystemA;
public class LicensePlateReader {
    public void scanPlate(String plate) {
        if (plate == null || !plate.matches("[A-Z0-9]{5,10}")) {
            throw new IllegalArgumentException("Invalid license plate");
        }
        System.out.println("License plate recognized: " + plate);
    }
}

