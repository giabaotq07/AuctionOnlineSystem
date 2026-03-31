import java.time.LocalDateTime;

public abstract class Item extends Entity{
    private String itemName;
    private String description;
    private double startingPrice;
    private double currentPrice;
    private String sellerId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    //Khoi tao
    public Item(String itemName, String description, double startingPrice,
                LocalDateTime startTime, LocalDateTime endTime, String sellerId) {
        super(); // Tự động lấy ID và createdAt từ Entity
        this.itemName = itemName;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice; // Lúc mới tạo, giá hiện tại = giá khởi điểm
        this.startTime = startTime;
        this.endTime = endTime;
        this.sellerId = sellerId;
    }
    //cac lop con phai tu phan loai
    public abstract String getCategory();
    //override printinfo
    public void printInfo() {
        System.out.println("--- Product Information ---");
        System.out.println("Name: " + itemName);
        System.out.println("Category: " + getCategory());
        System.out.println("Current Price: " + currentPrice);
        System.out.println("Ends at: " + endTime);
    }
    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public double getStartingPrice(){ return startingPrice;}


}