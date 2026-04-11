package Common;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionSession implements AuctionSubject, Serializable {
    private String sessionId;
    private Item item;
    private User seller;
    private AuctionStatus status;
    private LocalDateTime endTime;
    private List<AuctionObserver> observers = new ArrayList<>();
    // Lịch sử trả giá
    private List<Bid> bidHistory;
    @Override
    public void registerObserver(AuctionObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObserversNewBid(double price, String bidderName) {
        for (AuctionObserver observer : observers) {
            observer.onNewBidPlaced(item.getName(), price, bidderName);
        }
    }
    public AuctionSession(String sessionId, Item item, User seller, LocalDateTime endTime) {
        this.sessionId = sessionId;
        this.item = item;
        this.seller = seller;
        this.endTime = endTime;
        this.status = AuctionStatus.ACTIVE; // Vừa tạo là cho phép đấu giá luôn
        this.bidHistory = new ArrayList<>();
    }

    // 1. Hàm tìm giá cao nhất hiện tại
    public double getCurrentHighestPrice() {
        if (bidHistory.isEmpty()) {
            return item.getStartingPrice();
        }
        // Trả về số tiền của lượt bid cuối cùng trong danh sách
        return bidHistory.get(bidHistory.size() - 1).getAmount();
    }

    // 2. Logic xử lý ra giá
    public synchronized boolean placeBid(User bidder, double bidAmount) {
        // Kiểm tra thời gian
        if (LocalDateTime.now().isAfter(endTime)) {
            this.status = AuctionStatus.COMPLETED;
            System.out.println("Phiên đấu giá đã kết thúc!");
            return false;
        }

        // Kiểm tra trạng thái
        if (this.status != AuctionStatus.ACTIVE) {
            System.out.println("Phiên đấu giá không trong trạng thái hoạt động!");
            return false;
        }

        // Kiểm tra luật giá: Giá mới phải >= (Giá cao nhất hiện tại + Bước giá)
        double minimumRequiredPrice = getCurrentHighestPrice() + item.getStepPrice();
        if (bidAmount < minimumRequiredPrice) {
            System.out.println("Giá không hợp lệ! Bạn phải trả ít nhất: $" + minimumRequiredPrice);
            return false;
        }

        // Nếu hợp lệ -> Ghi nhận lượt trả giá
        Bid newBid = new Bid(bidder, bidAmount, LocalDateTime.now());
        bidHistory.add(newBid);
        System.out.println(bidder.getName() + " ra giá thành công: $" + bidAmount);

        notifyObserversNewBid(bidAmount, bidder.getName());
        return true;
    }
    @Override
    public String toString() {
        return item.getName() + " | Giá hiện tại: $" + getCurrentHighestPrice();
    }

    public Item getItem() {
        return item;
    }


    // 3. In lịch sử để kiểm tra
    public void printSessionSummary() {
        System.out.println("\n--- TỔNG KẾT PHIÊN ĐẤU GIÁ: " + item.getName() + " ---");
        for (Bid bid : bidHistory) {
            System.out.println("- " + bid.toString());
        }
        if (!bidHistory.isEmpty()) {
            System.out.println("Người đang giữ giá cao nhất: " + bidHistory.get(bidHistory.size() - 1).getBidder().getName());
        }
        System.out.println("------------------------------------------\n");
    }
}