package Common;

import java.time.LocalDateTime;

public class BidTransaction {
    private String bidderId;
    private double amount;
    private LocalDateTime time;

    public BidTransaction(String bidderId, double amount) {
        this.bidderId = bidderId;
        this.amount = amount;
        this.time = LocalDateTime.now();
    }

    public String getBidderId() { return bidderId; }
    public double getAmount() { return amount; }
    public LocalDateTime getTime() { return time; }
}