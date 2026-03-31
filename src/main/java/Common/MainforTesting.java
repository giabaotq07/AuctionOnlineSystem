package Common;

public class MainforTesting {
    public static void main(String[] args) {

        // =========================
        // TẠO HỆ THỐNG
        // =========================
        Auction auction = new Auction();

        // =========================
        // TẠO USER
        // =========================
        Bidder bidder1 = new Bidder("b1", "123", "Alice", 1000);
        Bidder bidder2 = new Bidder("b2", "123", "Bob", 800);

        Seller seller = new Seller("s1", "123", "Seller A");

        // add vào hệ thống
        auction.addBidder(bidder1);
        auction.addBidder(bidder2);

        // =========================
        // TẠO ITEM
        // =========================
        Item item1 = new Electronics(
                "Laptop",
                "Gaming laptop",
                100.0,
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now().plusDays(1),
                "s1",
                12 // ✔ warrantyMonths
        );


        auction.addItem(item1);

        // =========================
        // HIỂN THỊ
        // =========================
        System.out.println("=== ITEM LIST ===");
        auction.printItems();

        // =========================
        // ĐẤU GIÁ
        // =========================
        System.out.println("\n=== BIDDING ===");

        auction.placeBid("i1", "b1", 200);
        auction.placeBid("i1", "b2", 300);
        auction.placeBid("i1", "b1", 250); // fail (thấp hơn)

        // =========================
        // LỊCH SỬ BID
        // =========================
        System.out.println("\n=== BID HISTORY ===");
        auction.printBids("i1");

        // =========================
        // KẾT QUẢ
        // =========================
        System.out.println("\n=== RESULT ===");
        System.out.println("Highest Bid: " + auction.getHighestBid("i1"));
        System.out.println("Winner: " + auction.getWinner("i1"));

        // =========================
        // THÔNG TIN USER
        // =========================
        System.out.println("\n=== USER INFO ===");
        bidder1.printInfo();
        bidder2.printInfo();
    }
}