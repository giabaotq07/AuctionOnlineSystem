package Common;

import java.time.LocalDateTime;

public class Bid {
    private static Bid instance;

    private Bid() {}

    public static Bid getInstance() {
        if (instance == null) instance = new Bid();
        return instance;
    }

    // ===================== RESULT OBJECT =====================
    public static class BidResult {
        public final boolean success;
        public final String message;

        public BidResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    // ===================== BID LOGIC =====================
    public BidResult placeBid(Item item, double amount, String bidder) {

        // Kiểm tra 1: hết thời gian
        if (LocalDateTime.now().isAfter(item.getEndTime())) {
            return new BidResult(false, "Phiên đấu giá đã KẾT THÚC!");
        }

        // Kiểm tra 2: giá hợp lệ
        if (amount > item.getCurrentPrice()) {
            item.setCurrentPrice(amount);
            return new BidResult(true, bidder + " đang dẫn đầu với giá " + amount);
        }

        return new BidResult(false, "Giá quá thấp!");
    }
}