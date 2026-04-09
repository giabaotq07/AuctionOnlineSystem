package Common;

import java.time.LocalDateTime;

public class Bid {
    private User bidder;         // Ai là người trả giá?
    private double amount;       // Số tiền bao nhiêu?
    private LocalDateTime time;  // Trả giá lúc nào?

    public Bid(User bidder, double amount, LocalDateTime time) {
        this.bidder = bidder;
        this.amount = amount;
        this.time = time;
    }

    public User getBidder() { return bidder; }
    public double getAmount() { return amount; }
    public LocalDateTime getTime() { return time; }

    @Override
    public String toString() {
        return bidder.getName() + " đã trả $" + amount + " vào lúc " + time.withNano(0);
    }
}