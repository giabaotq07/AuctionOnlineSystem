package Common;

import Common.*;
import java.time.LocalDateTime;

public class MainforTesting {
    public static void main(String[] args) {
        // 1. Khởi tạo AuctionManager (Singleton)
        AuctionManager manager = AuctionManager.getInstance();

        // 2. Tạo các loại sản phẩm khác nhau (Electronics, Vehicle, Art) [cite: 113, 142]
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        Electronics laptop = new Electronics(
                "MacBook Pro", "M3 Max, 64GB RAM", 3000.0,
                start, end, "Seller_01", 12
        );

        Vehicle car = new Vehicle(
                "Tesla Model 3", "Like new 99%", 45000.0,
                start, end, "Seller_02",
                "Tesla", "Model 3", 2023, 5000.0
        );

        // 3. Tạo phiên đấu giá [cite: 116]
        Auction laptopAuction = new Auction(laptop);
        Auction carAuction = new Auction(car);

        // Đưa vào trình quản lý
        manager.addAuction(laptopAuction);
        manager.addAuction(carAuction);

        // QUAN TRỌNG: Cập nhật trạng thái sang RUNNING để có thể placeBid
        // (Bạn cần thêm public setter cho status trong lớp Auction nếu chưa có,
        // hoặc tạm thời sửa trực tiếp biến status trong Auction.java thành "RUNNING")
        // Giả sử logic của bạn đã cho phép phiên bắt đầu:
        // laptopAuction.setStatus("RUNNING");

        // 4. Tạo người dùng (Bidder) [cite: 35, 115]
        Bidder tung = new Bidder("tung_duong", "pass123", "Nguyễn Tùng", 50000.0);
        Bidder nam = new Bidder("hoang_nam", "pass456", "Hoàng Nam", 40000.0);

        // 5. Kiểm tra thông tin trước khi đấu giá [cite: 121]
        System.out.println("--- THÔNG TIN SẢN PHẨM ---");
        laptopAuction.printInfo();
        System.out.println();

        // 6. Mô phỏng đặt giá (Testing Logic) [cite: 47, 48]
        System.out.println("--- TIẾN TRÌNH ĐẤU GIÁ ---");

        // Lần 1: Tùng đặt giá hợp lệ
        if (laptopAuction.placeBid(tung, 3500.0)) {
            System.out.println("Tùng đặt giá 3500.0 thành công.");
        }

        // Lần 2: Nam đặt giá thấp hơn giá hiện tại (Mong đợi: Thất bại) [cite: 49, 58]
        if (!laptopAuction.placeBid(nam, 3200.0)) {
            System.out.println("Nam đặt 3200.0 thất bại (Giá thấp hơn hiện tại).");
        }

        // Lần 3: Nam đặt giá cao hơn [cite: 50]
        if (laptopAuction.placeBid(nam, 4000.0)) {
            System.out.println("Nam đặt giá 4000.0 thành công.");
        }

        // 7. Kết quả cuối cùng [cite: 54, 101]
        System.out.println("\n--- KẾT QUẢ HIỆN TẠI ---");
        System.out.println("Giá cao nhất: " + laptopAuction.getHighestBidAmount());
        System.out.println("ID người dẫn đầu: " + laptopAuction.getWinnerId());

        System.out.println("\n--- CHI TIẾT SẢN PHẨM SAU ĐẤU GIÁ ---");
        laptopAuction.printInfo();
    }
}