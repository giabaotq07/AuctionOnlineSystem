package app.models;

public class Seller extends User implements AuctionObserver {
  public Seller(int id, String name, Account account, Wallet wallet) {
    super(id, name, account, wallet);
  }

  public Seller(String name, Account account, Wallet wallet) {
    super(name, account, wallet);
  }

  @Override
  public UserRole getRole() {
    return UserRole.SELLER;
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
