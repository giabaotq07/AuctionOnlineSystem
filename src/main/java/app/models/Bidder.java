package app.models;

public class Bidder extends User implements AuctionObserver {
  public Bidder(int id, String username, String account, String password) {
    super(id, username, account, password);
  }

  @Override
  public void onNewBidPlaced(String itemName, double newPrice, String bidderName) {
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
  public void onAuctionClosed(String itemName, String winnerName, double finalPrice) {
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
