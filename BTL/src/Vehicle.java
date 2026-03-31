import java.time.LocalDateTime;

public class Vehicle extends Item {
    private String brand;
    private String model;
    private int year;
    private double usedRoad; // Thay thế mileage bằng usedRoad

    public Vehicle(String itemName, String description, double startingPrice,
                   LocalDateTime startTime, LocalDateTime endTime,
                   String sellerId, String brand, String model, int year, double usedRoad) {
        // Gọi constructor của lớp cha Item
        super(itemName, description, startingPrice, startTime, endTime, sellerId);
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.usedRoad = usedRoad;
    }

    @Override
    public String getCategory() {
        return "Vehicle";
    }

    @Override
    public void printInfo() {
        super.printInfo(); // Gọi phương thức in thông tin chung của Item [cite: 121]
        System.out.println("Brand: " + brand + " | Model: " + model);
        System.out.println("Year: " + year + " | Used Road: " + usedRoad + " km");
    }

    // Getters và Setters đảm bảo tính đóng gói (Encapsulation)
    public double getUsedRoad() {
        return usedRoad;
    }

    public void setUsedRoad(double usedRoad) {
        this.usedRoad = usedRoad;
    }

    // ... các Getters/Setters cho brand, model, year [cite: 119]
}