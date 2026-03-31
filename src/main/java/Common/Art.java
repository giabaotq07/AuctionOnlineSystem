package Common;
import java.time.LocalDateTime;
public class Art extends Item {
    private String Material;

    public Art(String itemName, String description, double startingPrice,
                       LocalDateTime startTime, LocalDateTime endTime,
                       String sellerId, String Material) {
        super(itemName, description, startingPrice, startTime, endTime, sellerId);
        this.Material = Material;
    }

    public String getCategory() {
        return "Art";
    }

    public String getMaterial() {
        return Material;
    }

    public void setMaterial(String Material) {
        this.Material = Material;
    }

    @Override
    public void printInfo() {
        super.printInfo(); // Gọi lại hàm in của Item [cite: 121]
    }
}
