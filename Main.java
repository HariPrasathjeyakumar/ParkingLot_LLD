import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        List<ParkingSpot> spots = new ArrayList<>();
        spots.add(new ParkingSpot(1, VehicleType.BIKE));
        spots.add(new ParkingSpot(2, VehicleType.BIKE));
        spots.add(new ParkingSpot(3, VehicleType.CAR));
        spots.add(new ParkingSpot(4, VehicleType.CAR));
        spots.add(new ParkingSpot(5, VehicleType.TRUCK));

        ParkingLot lot = new ParkingLot(spots, new SimpleFeeStrategy(20)); // ₹20/hr

        while (true) {
            System.out.println("\n=====PARKING LOT MENU=====");
            System.out.println("1. Park Vehicle");
            System.out.println("2. Exit Vehicle (Pay)");
            System.out.println("3. Show Available Spots");
            System.out.println("4. Show Active Tickets");
            System.out.println("5. Exit Program");
            System.out.print("Enter choice: ");

            int choice;
            try {
                choice = Integer.parseInt(sc.nextLine().trim());
            } catch (Exception e) {
                System.out.println("Invalid input!");
                continue;
            }

            if (choice == 1) {
                System.out.print("Enter vehicle type (BIKE/CAR/TRUCK): ");
                String typeStr = sc.nextLine().trim().toUpperCase();

                VehicleType type;
                try {
                    type = VehicleType.valueOf(typeStr);
                } catch (Exception e) {
                    System.out.println("Invalid vehicle type!");
                    continue;
                }

                System.out.print("Enter license number: ");
                String license = sc.nextLine().trim();

                Ticket t = lot.parkVehicle(new Vehicle(license, type));
                if (t == null) {
                    System.out.println("❌No available spot for " + type);
                } else {
                    System.out.println("✅Parked Successfully!");
                    System.out.println("Ticket ID: " + t.getTicketId());
                    System.out.println("Spot ID: " + t.getSpotId());
                    System.out.println("Entry Time: " + t.getEntryTime());
                }

            } else if (choice == 2) {
                System.out.print("Enter ticket id: ");
                int ticketId;
                try {
                    ticketId = Integer.parseInt(sc.nextLine().trim());
                } catch (Exception e) {
                    System.out.println("Invalid ticket id!");
                    continue;
                }

                ExitReceipt receipt = lot.exitVehicle(ticketId);
                if (receipt == null) {
                    System.out.println("❌ Ticket not found / already closed.");
                } else {
                    System.out.println("✅ Vehicle Exited!");
                    System.out.println("Ticket ID: " + receipt.ticketId);
                    System.out.println("Spot ID: " + receipt.spotId);
                    System.out.println("Hours Charged: " + receipt.hoursCharged);
                    System.out.println("Fee: ₹" + receipt.fee);
                }

            } else if (choice == 3) {
                lot.printAvailableSpots();

            } else if (choice == 4) {
                lot.printActiveTickets();

            } else if (choice == 5) {
                System.out.println("Bye!");
                break;

            } else {
                System.out.println("Invalid choice!");
            }
        }

        sc.close();
    }
}


enum VehicleType { BIKE, CAR, TRUCK }

class Vehicle {
    private final String licenseNumber;
    private final VehicleType type;

    public Vehicle(String licenseNumber, VehicleType type) {
        this.licenseNumber = licenseNumber;
        this.type = type;
    }

    public String getLicenseNumber() { return licenseNumber; }
    public VehicleType getType() { return type; }
}

class ParkingSpot {
    private final int spotId;
    private final VehicleType spotType;
    private boolean occupied;
    private Vehicle parkedVehicle;

    public ParkingSpot(int spotId, VehicleType spotType) {
        this.spotId = spotId;
        this.spotType = spotType;
        this.occupied = false;
    }

    public int getSpotId() { return spotId; }
    public VehicleType getSpotType() { return spotType; }
    public boolean isOccupied() { return occupied; }

    public boolean canFit(Vehicle v) {
        return !occupied && v.getType() == spotType;
    }

    public void park(Vehicle v) {
        this.parkedVehicle = v;
        this.occupied = true;
    }

    public Vehicle unpark() {
        Vehicle v = this.parkedVehicle;
        this.parkedVehicle = null;
        this.occupied = false;
        return v;
    }
}

class Ticket {
    private final int ticketId;
    private final String licenseNumber;
    private final VehicleType vehicleType;
    private final int spotId;
    private final LocalDateTime entryTime;

    public Ticket(int ticketId, Vehicle vehicle, int spotId) {
        this.ticketId = ticketId;
        this.licenseNumber = vehicle.getLicenseNumber();
        this.vehicleType = vehicle.getType();
        this.spotId = spotId;
        this.entryTime = LocalDateTime.now();
    }

    public int getTicketId() { return ticketId; }
    public String getLicenseNumber() { return licenseNumber; }
    public VehicleType getVehicleType() { return vehicleType; }
    public int getSpotId() { return spotId; }
    public LocalDateTime getEntryTime() { return entryTime; }
}


interface FeeStrategy {
    long calculateFee(LocalDateTime entry, LocalDateTime exit, VehicleType type);
}

class SimpleFeeStrategy implements FeeStrategy {
    private final long ratePerHour;

    public SimpleFeeStrategy(long ratePerHour) {
        this.ratePerHour = ratePerHour;
    }

    @Override
    public long calculateFee(LocalDateTime entry, LocalDateTime exit, VehicleType type) {
        long minutes = Duration.between(entry, exit).toMinutes();
        long hours = (minutes + 59) / 60; // ceil to next hour
        if (hours <= 0) hours = 1;
        return hours * ratePerHour;
    }
}


class ExitReceipt {
    final int ticketId;
    final int spotId;
    final long hoursCharged;
    final long fee;

    ExitReceipt(int ticketId, int spotId, long hoursCharged, long fee) {
        this.ticketId = ticketId;
        this.spotId = spotId;
        this.hoursCharged = hoursCharged;
        this.fee = fee;
    }
}

class ParkingLot {
    private final List<ParkingSpot> spots;
    private final Map<Integer, Ticket> activeTickets = new HashMap<>();
    private final FeeStrategy feeStrategy;

    private int ticketCounter = 0;

    public ParkingLot(List<ParkingSpot> spots, FeeStrategy feeStrategy) {
        this.spots = spots;
        this.feeStrategy = feeStrategy;
    }

    public Ticket parkVehicle(Vehicle v) {
        ParkingSpot spot = findSpot(v);
        if (spot == null) return null;

        spot.park(v);
        int id = ++ticketCounter;
        Ticket t = new Ticket(id, v, spot.getSpotId());
        activeTickets.put(id, t);
        return t;
    }

    public ExitReceipt exitVehicle(int ticketId) {
        Ticket t = activeTickets.remove(ticketId);
        if (t == null) return null;

        ParkingSpot spot = getSpotById(t.getSpotId());
        if (spot == null || !spot.isOccupied()) return null;

        LocalDateTime exitTime = LocalDateTime.now();
        long fee = feeStrategy.calculateFee(t.getEntryTime(), exitTime, t.getVehicleType());

        long minutes = Duration.between(t.getEntryTime(), exitTime).toMinutes();
        long hoursCharged = (minutes + 59) / 60;
        if (hoursCharged <= 0) hoursCharged = 1;

        spot.unpark();
        return new ExitReceipt(t.getTicketId(), t.getSpotId(), hoursCharged, fee);
    }

    private ParkingSpot findSpot(Vehicle v) {
        for (ParkingSpot s : spots) {
            if (s.canFit(v)) return s;
        }
        return null;
    }

    private ParkingSpot getSpotById(int id) {
        for (ParkingSpot s : spots) {
            if (s.getSpotId() == id) return s;
        }
        return null;
    }

    public void printAvailableSpots() {
        System.out.println("\n---Available Spots---");
        boolean any = false;
        for (ParkingSpot s : spots) {
            if (!s.isOccupied()) {
                any = true;
                System.out.println("Spot " + s.getSpotId() + " (" + s.getSpotType() + ")");
            }
        }
        if (!any) System.out.println("No spots available.");
    }

    public void printActiveTickets() {
        System.out.println("\n--- Active Tickets ---");
        if (activeTickets.isEmpty()) {
            System.out.println("No active tickets.");
            return;
        }
        for (Ticket t : activeTickets.values()) {
            System.out.println("Ticket " + t.getTicketId() +
                    " | " + t.getVehicleType() +
                    " | " + t.getLicenseNumber() +
                    " | Spot " + t.getSpotId() +
                    " | Entry " + t.getEntryTime());
        }
    }
}
