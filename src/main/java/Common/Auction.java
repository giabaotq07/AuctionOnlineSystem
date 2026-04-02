package Common;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Auction extends Entity {
    private Item item;
    private String status = "OPEN"; // [cite: 55]
    // Sử dụng CopyOnWriteArrayList để an toàn khi vừa đọc (vẽ biểu đồ) vừa ghi (đặt giá)
    private List<BidTransaction> bidHistory = new CopyOnWriteArrayList<>();

    public Auction(Item item) {
        super(UUID.randomUUID().toString());
        this.item = item;
    }

    // Logic đặt giá tập trung cho 1 sản phẩm
    public synchronized boolean placeBid(Bidder bidder, double amount) {
        // 1. Kiểm tra trạng thái phiên [cite: 59]
        if (!status.equals("RUNNING")) {
            System.out.println("Phiên đấu giá không trong trạng thái hoạt động");
            return false;
        }

        // 2. Kiểm tra giá cao nhất hiện tại [cite: 48, 49]
        double currentMax = getHighestBidAmount();
        if (amount <= currentMax) {
            System.out.println("Giá đặt phải cao hơn giá hiện tại: " + currentMax);
            return false;
        }

        // 3. Kiểm tra số dư người dùng
        if (!bidder.canAfford(amount)) {
            System.out.println("Tài khoản không đủ số dư");
            return false;
        }

        // 4. Lưu giao dịch [cite: 117]
        BidTransaction bid = new BidTransaction(this.id, bidder.getId(), amount);
        bidHistory.add(bid);

        // Cập nhật giá hiện tại vào Item để đồng bộ [cite: 44, 50]
        item.setCurrentPrice(amount);

        return true;
    }

    public double getHighestBidAmount() {
        if (bidHistory.isEmpty()) return item.getStartingPrice(); // [cite: 42]
        return bidHistory.get(bidHistory.size() - 1).getAmount();
    }

    public String getWinnerId() {
        if (bidHistory.isEmpty()) return null;
        // Lấy người cuối cùng đặt giá cao nhất [cite: 54]
        return bidHistory.get(bidHistory.size() - 1).getBidderId();
    }

    @Override
    public void printInfo() {
        item.printInfo();
        System.out.println("Trạng thái: " + status);
        System.out.println("Số lượt bid: " + bidHistory.size());
    }
}