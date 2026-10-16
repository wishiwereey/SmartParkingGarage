import Persistence.*;
import Facade.*;
import SubsystemA.*;
import SubsystemB.*;
import SubsystemC.*;
import org.junit.jupiter.api.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import SmartParking.Main;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
public class ParkingTest {
    private Path file;
    private ParkingFacade facade;
    private ParkingAdminFacade admin;
    private OperationRepository operations;
    private SpotRepository spots;
    private EntryRepository entries;
    private PaymentRepository payments;
    @BeforeEach
    void setup() throws Exception {
        file = Files.createTempFile("parking-test-", ".db");
        Database.setUrl("jdbc:sqlite:" + file);
        Database.initialize();
        facade = new ParkingFacade();
        admin = new ParkingAdminFacade();
        operations = new OperationRepository();
        spots = new SpotRepository();
        entries = new EntryRepository();
        payments = new PaymentRepository();
    }
    @AfterEach
    void cleanup() throws Exception {
        Files.deleteIfExists(file);
    }
    private void givenFreeSpot(int number) {
        spots.save(new ParkingSpot(number));
    }
    private int latest(String plate) {
        return operations.findLatestIdByPlate(plate);
    }
    @Test
    void successfulParking() {
        givenFreeSpot(1);
        int id = facade.parkCar("ABC123", 1, 500, false, false);
        assertEquals("COMPLETED", operations.getStatus(id));
        assertEquals("ENTERED", entries.findByOperation(id).getStatus());
        assertEquals("RESERVED", spots.findByOperation(id).getStatus());
        assertEquals("PAID", payments.findByOperation(id).getStatus());
        assertThrows(IllegalStateException.class, () -> admin.compensateOperation(id));
    }
    @Test
    void bFailureCompensatesA() {
        assertThrows(IllegalStateException.class, () -> facade.parkCar("BBB123", 2, 500, true, false));
        int id = latest("BBB123");
        assertEquals("COMPENSATED", operations.getStatus(id));
        assertEquals("CANCELLED", entries.findByOperation(id).getStatus());
        assertNull(payments.findByOperation(id));
    }
    @Test
    void cFailureCompensatesBThenA() {
        givenFreeSpot(3);
        assertThrows(IllegalStateException.class, () -> facade.parkCar("CCC123", 3, 500, false, true));
        int id = latest("CCC123");
        assertEquals("COMPENSATED", operations.getStatus(id));
        assertEquals("CANCELLED", entries.findByOperation(id).getStatus());
        assertEquals("AVAILABLE", spots.findByNumber(3).getStatus());
        assertEquals("CANCELLED", payments.findByOperation(id).getStatus());
    }
    @Test
    void unavailableSpotCompensatesEntry() {
        givenFreeSpot(4);
        int first = facade.parkCar("FIRST1", 4, 100, false, false);
        assertThrows(IllegalStateException.class, () -> facade.parkCar("SECOND2", 4, 100, false, false));
        int id = latest("SECOND2");
        assertEquals("COMPENSATED", operations.getStatus(id));
        assertEquals("CANCELLED", entries.findByOperation(id).getStatus());
        assertEquals("COMPLETED", operations.getStatus(first));
    }
    @Test
    void restartRecoversPersistedB() {
        givenFreeSpot(5);
        int id = operations.create("RESTART", 5, 200);
        ParkingEntry entry = new ParkingEntry("RESTART");
        entry.confirmEntry();
        entries.saveForOperation(id, entry);
        spots.reserveForOperation(id, 5);
        operations.updateStatus(id, "B_DONE");
        Database.initialize();
        assertEquals(1, new ParkingRecoveryService().recover());
        assertEquals("COMPENSATED", operations.getStatus(id));
        assertEquals("CANCELLED", entries.findByOperation(id).getStatus());
        assertEquals("AVAILABLE", spots.findByNumber(5).getStatus());
        assertEquals(0, new ParkingRecoveryService().recover());
    }
    @Test
    void restartRecoversActionBeforeProgressUpdate() {
        givenFreeSpot(6);
        int id = operations.create("PARTIAL", 6, 200);
        ParkingEntry entry = new ParkingEntry("PARTIAL");
        entry.confirmEntry();
        entries.saveForOperation(id, entry);
        spots.reserveForOperation(id, 6);
        assertEquals("NEW", operations.getStatus(id));
        assertEquals(1, new ParkingRecoveryService().recover());
        assertEquals("AVAILABLE", spots.findByNumber(6).getStatus());
        assertEquals("CANCELLED", entries.findByOperation(id).getStatus());
    }
    @Test
    void manualCompensationAndInspection() {
        givenFreeSpot(7);
        int id = operations.create("ADMIN1", 7, 200);
        ParkingEntry entry = new ParkingEntry("ADMIN1");
        entry.confirmEntry();
        entries.saveForOperation(id, entry);
        spots.reserveForOperation(id, 7);
        operations.updateStatus(id, "B_DONE");
        assertTrue(admin.inspectOperation(id).contains("RESERVED"));
        admin.compensateOperation(id);
        admin.compensateOperation(id);
        assertEquals("COMPENSATED", operations.getStatus(id));
    }

    @Test
    void paidInterruptedOperationRequiresRefund() {
        int id = operations.create("PAID01", 8, 100);
        ParkingPayment payment = new ParkingPayment("PAID01", 100);
        payment.pay();
        payments.saveForOperation(id, payment);
        assertThrows(IllegalStateException.class, () -> admin.compensateOperation(id));
        assertEquals("REFUND_REQUIRED", operations.getStatus(id));
        assertEquals(0, new ParkingRecoveryService().recover());
    }
    @Test
    void validatesInputsAndTransitions() {
        assertThrows(IllegalArgumentException.class, () -> facade.parkCar("bad!", 1, 100, false, false));
        assertThrows(IllegalArgumentException.class, () -> facade.parkCar("VALID1", 1, -1, false, false));
        assertThrows(IllegalArgumentException.class, () -> facade.parkCar("VALID1", 0, 100, false, false));
        ParkingEntry entry = new ParkingEntry("ABC123");
        assertThrows(IllegalStateException.class, entry::cancelEntry);
        entry.confirmEntry();
        assertThrows(IllegalStateException.class, entry::confirmEntry);
        entry.cancelEntry();
        ParkingSpot spot = new ParkingSpot(9);
        assertThrows(IllegalStateException.class, spot::occupy);
        spot.reserve();
        spot.occupy();
        spot.release();
        ParkingPayment payment = new ParkingPayment("ABC123", 20);
        payment.cancel();
        assertThrows(IllegalStateException.class, payment::pay);
    }
    @Test
    void repositoryFailuresAndStatusChecks() {
        assertThrows(IllegalArgumentException.class, () -> operations.get(9999));
        assertNull(entries.findByOperation(9999));
        assertNull(spots.findByOperation(9999));
        assertNull(payments.findByOperation(9999));
        assertThrows(IllegalStateException.class, () -> spots.reserveForOperation(99, 99));
        assertThrows(IllegalStateException.class, () -> entries.updateStatusByOperation(99, new ParkingEntry("ABC123")));
        assertThrows(IllegalStateException.class, () -> payments.updateStatusByOperation(99, new ParkingPayment("ABC123", 100)));
    }
    @Test
    void mainMenuTest() {
        InputStream originalInput = System.in;
        String commands = String.join("\n",
                "1",
                "98765",
                "2",
                "MAINTEST1",
                "98765",
                "500",
                "5",
                "999999",
                "6",
                "999999",
                "abc",
                "0"
        ) + "\n";
        try {
            System.setIn(new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8)));
            assertDoesNotThrow(() -> Main.main(new String[0]));
        } finally {
            System.setIn(originalInput);
        }
    }

}
