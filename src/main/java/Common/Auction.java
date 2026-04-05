package Common;

import java.util.*;
import java.time.LocalDateTime;

public class Auction {
    private static Auction instance;
    private List<Item> itemList = new ArrayList<>();
    // Dùng 1 Scanner duy nhất cho cả class để tránh lỗi trôi lệnh
    private Scanner sc = new Scanner(System.in);

    private Auction() {}

    public static Auction getInstance() {
        if (instance == null) instance = new Auction();
        return instance;
    }

    // NHẬP: Thêm mặt hàng (Lên mẫu trực tiếp)
    public void addItem() {
        try {
            System.out.println("Nhập theo thứ tự: [Tên] [Loại] [Giá_Khởi_Điểm] [Số_Phút_Đấu_Giá]");
            String name = sc.next();
            String cat = sc.next();
            double price = sc.nextDouble();
            int minutes = sc.nextInt(); // Nhập số phút để kiểm tra date dễ hơn

            Item newItem = new Item(name, cat, price, LocalDateTime.now().plusMinutes(minutes));
            itemList.add(newItem);
            System.out.println("=> Đã nạp mẫu hàng: " + name + " (" + cat + ")");
        } catch (InputMismatchException e) {
            System.out.println("!! Lỗi: Giá và thời gian phải là số. Thao tác bị hủy.");
            sc.nextLine(); // Dọn dẹp bộ nhớ đệm bị lỗi
        }
    }

    // XỬ LÝ CHÍNH: Nhập - Xuất điều hướng
    public void xulydulieu() {
        while (true) {
            System.out.println("""
                    \n--- MENU QUẢN LÝ ĐẤU GIÁ ---
                    1. Add item (Thêm hàng)
                    2. Bid item (Đấu giá)
                    3. Show all (Xem danh sách)
                    0. Exit (Thoát)
                    """);
            try {
                int choice = sc.nextInt();
                if (choice == 0) break;

                switch (choice) {
                    case 1 -> this.addItem();
                    case 2 -> this.bidLogic();
                    case 3 -> this.showStatus();
                    default -> System.out.println("Lựa chọn không hợp lệ!");
                }
            } catch (InputMismatchException e) {
                System.out.println("!! Lỗi: Vui lòng chỉ nhập số từ 0-3.");
                sc.nextLine(); // Dọn dẹp để không bị lặp lỗi vô tận
            }
        }
    }

    // Tách riêng logic Bid để file Auction gọn gàng hơn (SRP)
    private void bidLogic() {
        if (itemList.isEmpty()) {
            System.out.println("Chưa có hàng hóa nào!");
            return;
        }

        this.showStatus();
        try {
            System.out.print("Chọn ID món hàng: ");
            int id = sc.nextInt();

            if (id >= 0 && id < itemList.size()) {
                System.out.println("Nhập [Tên_Bạn] và [Số_Tiền]:");
                String bidderName = sc.next();
                double amount = sc.nextDouble();

                // GỌI SINGLETON BID để xử lý logic kiểm tra giá và DATE
                boolean success = Bid.getInstance().placeBid(itemList.get(id), amount, bidderName);

                if (success) System.out.println("=> Chúc mừng! Bạn dẫn đầu.");
            } else {
                System.out.println("!! ID không tồn tại.");
            }
        } catch (InputMismatchException e) {
            System.out.println("!! Lỗi nhập số tiền/ID.");
            sc.nextLine();
        }
    }

    public void showStatus() {
        System.out.println("\n--- DANH SÁCH MẶT HÀNG ---");
        for (int i = 0; i < itemList.size(); i++) {
            System.out.print("ID: " + i + " ");
            itemList.get(i).printInfo();
        }
    }
}