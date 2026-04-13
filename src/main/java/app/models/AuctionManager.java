package app.models;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
  // Biến static duy nhất của lớp
  private static AuctionManager instance;
  // Sử dụng ConcurrentHashMap để an toàn khi nhiều luồng truy cập cùng lúc
  private Map<String, Auction> auctions;

  // Constructor private để ngăn chặn khởi tạo từ bên ngoài
  private AuctionManager() {
    auctions = new ConcurrentHashMap<>();
  }

  // Phương thức static để lấy instance duy nhất
  public static synchronized AuctionManager getInstance() {
    if (instance == null) {
      instance = new AuctionManager();
    }
    return instance;
  }

  public void addAuction(Auction auction) {
    auctions.put(auction.getId(), auction);
  }

  public Auction getAuction(String id) {
    return auctions.get(id);
  }
}
