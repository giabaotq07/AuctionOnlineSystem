package app;

import app.dao.*;
import app.enums.HistoryType;
import app.enums.ItemType;
import app.enums.UserRole;
import app.exception.NotFoundException;
import app.exception.UserAlreadyExistsException;
import app.models.*;
import app.service.*;
import java.time.LocalDateTime;

public class DemoRunner {
  static void main() {
    System.out.println("=== BAT DAU DEMO CHAY THU HE THONG AUCTION ===");

    // Khoi tao Database va tao cac bang neu chua co
    System.out.println("Dang kiem tra va khoi tao Database...");
    UserDAO userDAO = new UserDAO();
    ItemDAO itemDAO = new ItemDAO();
    AuctionDAO sessionDAO = new AuctionDAO();
    BidDAO bidDAO = new BidDAO();
    HistoryDAO historyDAO = new HistoryDAO();

    UserService userService = new UserService(userDAO);
    ItemService itemService = new ItemService(itemDAO);
    AuctionService sessionService = new AuctionService(sessionDAO);
    BidService bidService = new BidService(bidDAO);
    HistoryService historyService = new HistoryService(historyDAO);
    User seller;
    User buyer1;
    User buyer2;
    // 1. Dang ky user
    System.out.println("\n1. Dang ky phien ban demo nguoi dung...");
    try {
      User user =
          UserFactory.createUser(
              "Nguoi Ban", new Account("nguoiban", "123456"), new Wallet(), UserRole.BIDDER);
      userService.register(user);
    } catch (UserAlreadyExistsException | NotFoundException e) {
      System.out.println(e.getMessage());
    }
    seller = userService.getUserByAccount("nguoiban");

    try {
      User user =
          UserFactory.createUser(
              "Nguoi Mua", new Account("nguoimua1", "123456"), new Wallet(), UserRole.BIDDER);
      userService.register(user);
    } catch (UserAlreadyExistsException | NotFoundException e) {
      System.out.println(e.getMessage());
    }
    buyer1 = userService.getUserByAccount("nguoimua1");

    try {
      User user =
          UserFactory.createUser(
              "Nguoi Mua", new Account("nguoimua2", "123456"), new Wallet(), UserRole.BIDDER);
      userService.register(user);
    } catch (UserAlreadyExistsException | NotFoundException e) {
      System.out.println(e.getMessage());
    }
    buyer2 = userService.getUserByAccount("nguoimua2");
    // Kiem tra looi DB neu co
    if (seller == null || buyer1 == null || buyer2 == null) {
      System.out.println("Loi tao DB User! Vui long kiem tra MySQL (app.config.connection).");
      return;
    }
    // 2. Dang san pham (ID se duoc AUTO_INCREMENT)
    System.out.println("\n2. Dang san pham moi...");
    Item phone =
        ItemFactory.createItem(
            "IPhone 16 Pro Max", "Dien thoai moi", 1000, 50, ItemType.ELECTRONICS);
    phone = itemService.add(phone);
    System.out.println("San pham " + phone.getName() + " da dang voi ID: " + phone.getId());
    // 3. Tao phien dau gia
    System.out.println("\n3. Mo phien dau gia (Ket thuc sau 5 giay)...");
    Auction session = new Auction(phone, seller, LocalDateTime.now().plusSeconds(5));
    session = sessionService.createAuctionSession(session);
    System.out.println("Phien dau gia tao voi ID: " + session.getId());
    historyService.logEvent(
        session.getId(), HistoryType.ADD_ITEM, "Nguoi ban " + seller.getName() + " da mo phien.");
    // 4. Mua ban (Bidding)
    System.out.println("\n4. Nguoi mua bat dau dat gia...");
    bidService.placeBid(session.getId(), buyer1.getId(), 1050);
    historyService.logEvent(session.getId(), HistoryType.BID, buyer1.getName() + " tra 1050");
    bidService.placeBid(session.getId(), buyer2.getId(), 1200);
    historyService.logEvent(session.getId(), HistoryType.BID, buyer2.getName() + " tra 1200");
    bidService.placeBid(session.getId(), buyer1.getId(), 1300);
    historyService.logEvent(session.getId(), HistoryType.BID, buyer1.getName() + " tra 1300");
    // 5. In lich su phien
    System.out.println("\n5. Kiem tra lich su phien:");
    for (HistoryRecord rec : historyService.getSessionHistory(session.getId())) {
      System.out.println("- [" + rec.getTime() + "] " + rec.getType() + ": " + rec.getMessage());
    }
    // 6. Cho doi ket thuc
    System.out.println("\n6. Cho 5.5 giay de phien het han...");
    try {
      Thread.sleep(5500);
    } catch (Exception e) {
    }
    sessionService.handleSessionCompletion(session.getId(), bidService);
  }
}
