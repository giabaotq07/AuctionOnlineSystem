package app;

import app.dao.AuctionDAO;
import app.dao.AutoBidDAO;
import app.dao.BidDAO;
import app.dao.ItemDAO;
import app.dao.UserDAO;
import app.dao.impl.MySqlAuctionDAO;
import app.dao.impl.MySqlAutoBidDAO;
import app.dao.impl.MySqlBidDAO;
import app.dao.impl.MySqlItemDAO;
import app.dao.impl.MySqlUserDAO;
import app.enums.AuctionStatus;
import app.enums.ItemStatus;
import app.enums.ItemType;
import app.enums.UserRole;
import app.exception.ServiceException;
import app.models.*;
import app.service.*;
import java.time.LocalDateTime;

public class DemoRunner {

  static void main() {
    System.out.println("=== BAT DAU DEMO CHAY THU HE THONG AUCTION ===");
    System.out.println("Dang kiem tra va khoi tao Database...");
    UserDAO userDAO = new MySqlUserDAO();
    ItemDAO itemDAO = new MySqlItemDAO();
    AuctionDAO sessionDAO = new MySqlAuctionDAO();
    AutoBidDAO autoBidDAO = new MySqlAutoBidDAO();
    BidDAO bidDAO = new MySqlBidDAO();

    UserService userService = new UserService(userDAO);
    ItemService itemService = new ItemService(itemDAO);
    AuctionService sessionService = new AuctionService(sessionDAO, bidDAO, itemDAO);
    BidService bidService = new BidService(bidDAO, autoBidDAO, sessionDAO, itemDAO);
    User seller;
    User buyer1;
    User buyer2;
    // 1. Dang ky user
    System.out.println("\n1. Dang ky phien ban demo nguoi dung...");
    try {
      seller =
          UserFactory.createUser(
              "Nguoi Ban", new Account("nguoiban", "123456"), new Wallet(), UserRole.SELLER);
      seller = userService.register(seller);
    } catch (ServiceException e) {
      System.out.println(e.getMessage());
      seller = userService.login("nguoiban", "123456");
    }

    try {
      buyer1 =
          UserFactory.createUser(
              "Nguoi Mua", new Account("nguoimua1", "123456"), new Wallet(), UserRole.BIDDER);
      buyer1 = userService.register(buyer1);
    } catch (ServiceException e) {
      System.out.println(e.getMessage());
      buyer1 = userService.login("nguoimua1", "123456");
    }

    try {
      buyer2 =
          UserFactory.createUser(
              "Nguoi Mua", new Account("nguoimua2", "123456"), new Wallet(), UserRole.BIDDER);
      buyer2 = userService.register(buyer2);
    } catch (ServiceException e) {
      System.out.println(e.getMessage());
      buyer2 = userService.login("nguoimua2", "123456");
    }
    // Kiem tra looi DB neu co
    if (seller == null || buyer1 == null || buyer2 == null) {
      System.out.println("Loi tao DB User! Vui long kiem tra MySQL.");
      return;
    }
    // 2. Dang san pham (ID se duoc AUTO_INCREMENT)
    System.out.println("\n2. Dang san pham moi...");
    long startingPrice = 1000;
    long stepPrice = 10;
    Item phone =
        ItemFactory.createItem(
            "IPhone 16 Pro Max",
            seller.getId(),
            "Dien thoai moi",
            startingPrice,
            stepPrice,
            ItemType.ELECTRONICS);
    phone = itemService.add(phone);
    System.out.println("San pham " + phone.getName() + " da dang voi ID: " + phone.getId());
    // 3. Tao phien dau gia
    System.out.println("\n3. Mo phien dau gia (Ket thuc sau 5 giay)...");
    Auction session =
        new Auction(
            phone.getId(),
            seller.getId(),
            LocalDateTime.now().plusSeconds(5),
            phone.getStartingPrice());
    session = sessionService.createAuction(session);
    System.out.println("Phien dau gia tao voi ID: " + session.getId());
    // bắt đầu phiên
    sessionService.updateStatus(session.getId(), AuctionStatus.RUNNING);
    session.start();
    sessionService.setStartTime(session.getId(), LocalDateTime.now());
    phone.setStatus(ItemStatus.UNDER_AUCTION);
    itemService.updateStatus(phone.getId(), ItemStatus.UNDER_AUCTION);
    // 4. Mua ban (Bidding)
    System.out.println("\n4. Nguoi mua bat dau dat gia...");
    bidService.placeBid(session.getId(), buyer1.getId(), 1050);
    bidService.placeBid(session.getId(), buyer2.getId(), 1200);
    bidService.placeBid(session.getId(), buyer1.getId(), 1300);
    // 6. Cho doi ket thuc
    System.out.println("\n6. Cho 5.5 giay de phien het han...");
    try {
      sessionService.setEndTime(session.getId(), LocalDateTime.now().plusSeconds(5));
      Thread.sleep(5500);
    } catch (Exception _) {
    }
    sessionService.handleCompletion(session.getId());
    // giả sử đã bán
    phone.setStatus(ItemStatus.SOLD);
    itemService.updateStatus(phone.getId(), ItemStatus.SOLD);
  }
}
