package app.models;

import app.enums.UserRole;
import app.observer.AuctionObserver;

public class Bidder extends User implements AuctionObserver {
  public Bidder(int id, String name, Account account, Wallet wallet) {
    super(id, name, account, wallet);
    role = UserRole.BIDDER;
  }

  public Bidder(String name, Account account, Wallet wallet) {
    super(name, account, wallet);
    role = UserRole.BIDDER;
  }

  @Override
  public UserRole getRole() {
    return UserRole.BIDDER;
  }

  @Override
  public void onNewBidPlaced(String itemName, long newPrice, String bidderName) {
    if (this.getName().equals(bidderName)) {
      System.out.println("[" + getName() + "]: Bạn đã giữ giá cao nhất mới: $" + newPrice);
    } else {
      System.out.println(
          "["
              + getName()
              + "]: Thông báo! '"
              + bidderName
              + "' vừa trả giá $"
              + newPrice
              + " cho "
              + itemName);
    }
  }

  @Override
  public void onAuctionClosed(String itemName, String winnerName, long finalPrice) {
    if (this.getName().equals(winnerName)) {
      System.out.println(
          "["
              + getName()
              + "]: CHÚC MỪNG! Bạn đã thắng đấu giá "
              + itemName
              + " với giá $"
              + finalPrice);
    } else {
      System.out.println(
          "["
              + getName()
              + "]: Phiên đấu giá "
              + itemName
              + " đã kết thúc. Người thắng: "
              + winnerName);
    }
  }
}
