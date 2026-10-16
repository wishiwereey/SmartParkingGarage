package SmartParking;
import Persistence.*;
import Facade.*;
import SubsystemB.*;
import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Database.initialize();
        ParkingFacade parking = new ParkingFacade();
        ParkingAdminFacade admin = new ParkingAdminFacade();
        SpotRepository spots = new SpotRepository();
        System.out.println("Recovered operations: " + new ParkingRecoveryService().recover());
        Scanner input = new Scanner(System.in);
        while (true) {
            System.out.println();
            System.out.println("=== SMART PARKING GARAGE ===");
            System.out.println("1. Add parking spot");
            System.out.println("2. Park car");
            System.out.println("3. Simulate B failure");
            System.out.println("4. Simulate C failure");
            System.out.println("5. Inspect operation");
            System.out.println("6. Manual compensation");
            System.out.println("0. Exit");
            System.out.print("Choose: ");
            String choice = input.nextLine().trim();
            if ("0".equals(choice)) {
                break;
            }
            try {
                if ("1".equals(choice)) {
                    System.out.print("Spot number: ");
                    int number = Integer.parseInt(input.nextLine());
                    spots.save(new ParkingSpot(number));
                    System.out.println("Spot available: " + number);
                } else if ("2".equals(choice) || "3".equals(choice) || "4".equals(choice)) {
                    System.out.print("Plate: ");
                    String plate = input.nextLine().trim().toUpperCase();
                    System.out.print("Spot: ");
                    int spot = Integer.parseInt(input.nextLine());
                    System.out.print("Amount KZT: ");
                    double amount = Double.parseDouble(input.nextLine());
                    int id = parking.parkCar(plate, spot, amount, "3".equals(choice), "4".equals(choice));
                    System.out.println(admin.inspectOperation(id));
                } else if ("5".equals(choice) || "6".equals(choice)) {
                    System.out.print("Operation ID: ");
                    int id = Integer.parseInt(input.nextLine());
                    if ("6".equals(choice)) {
                        admin.compensateOperation(id);
                    }
                    System.out.println(admin.inspectOperation(id));
                } else {
                    System.out.println("Unknown option");
                }
            } catch (RuntimeException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}
