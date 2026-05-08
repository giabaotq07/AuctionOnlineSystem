package app.models;

import app.enums.UserRole;
import app.observer.AuctionObserver;

public class Seller extends User implements AuctionObserver {
  public Seller(int id, String name, Account account, Wallet wallet) {
    super(id, name, account, wallet);
    role = UserRole.SELLER;
  }

  public Seller(String name, Account account, Wallet wallet) {
    super(name, account, wallet);
    role = UserRole.SELLER;
  }

  @Override
  public UserRole getRole() {
    return UserRole.SELLER;
  }

  @Override
  public void onNewBidPlaced(String itemName, long newPrice, String bidderName) {
    System.out.println(
        "["
            + getName()
            + "]: Thông báo! '"
            + bidderName
            + "' vừa trả giá $"
            + newPrice
            + " cho mặt hàng của bạn: "
            + itemName);
  }

  @Override
  public void onAuctionClosed(String itemName, String winnerName, long finalPrice) {
    System.out.println(
        "["
            + getName()
            + "]: Phiên đấu giá cho mặt hàng của bạn '"
            + itemName
            + "' đã kết thúc. Người thắng: "
            + winnerName
            + " với giá $"
            + finalPrice);
  }
}
