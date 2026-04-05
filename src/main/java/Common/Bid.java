package Common;
import java.time.LocalDateTime;

public class Bid {
    private static Bid instance;
    private Bid() {}

    public static Bid getInstance() {
        if (instance == null) instance = new Bid();
        return instance;
    }

    public boolean placeBid(Item item, double amount, String bidder) {
        // Kiểm tra 1: Còn trong thời gian đấu giá không?
        if (LocalDateTime.now().isAfter(item.getEndTime())) {
            System.out.println(">>> LỖI: Phiên đấu giá cho món này đã KẾT THÚC!");
            return false;
        }

        // Kiểm tra 2: Giá có cao hơn giá hiện tại không?
        if (amount > item.getCurrentPrice()) {
            item.setCurrentPrice(amount);
            System.out.println(">>> CHẤP NHẬN: " + bidder + " dẫn đầu với giá " + amount);
            return true;
        }

        System.out.println(">>> TỪ CHỐI: Giá quá thấp!");
        return false;
    }
}