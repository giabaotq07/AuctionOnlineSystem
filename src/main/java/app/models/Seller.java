package app.models;

public class Seller extends User implements AuctionObserver {
  public Seller(int id, String username, String account, String password) {
    super(id, username, account, password);
  }

  @Override
  public void onNewBidPlaced(String itemName, double newPrice, String bidderName) {
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
  public void onAuctionClosed(String itemName, String winnerName, double finalPrice) {
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
