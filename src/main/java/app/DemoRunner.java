package app;

import app.DAO.AuctionDAO;
import app.DAO.AutoBidDAO;
import app.DAO.BidDAO;
import app.DAO.ItemDAO;
import app.DAO.UserDAO;
import app.DAO.impl.MySqlAuctionDAO;
import app.DAO.impl.MySqlAutoBidDAO;
import app.DAO.impl.MySqlBidDAO;
import app.DAO.impl.MySqlItemDAO;
import app.DAO.impl.MySqlUserDAO;
import app.database.TransactionManager;
import app.enums.AuctionStatus;
import app.enums.ItemStatus;
import app.enums.ItemType;
import app.enums.UserRole;
import app.exception.ServiceException;
import app.models.Account;
import app.models.Auction;
import app.models.Item;
import app.models.ItemFactory;
import app.models.User;
import app.models.UserFactory;
import app.models.Wallet;
import app.service.AntiSnipeService;
import app.service.AuctionService;
import app.service.BidService;
import app.service.BidValidator;
import app.service.ItemService;
import app.service.UserService;
import java.time.LocalDateTime;

/** DemoRunner. */
public class DemoRunner {
  static void main() {
    System.out.println("=== BAT DAU DEMO CHAY THU HE THONG AUCTION ===");
    System.out.println("Dang kiem tra va khoi tao Database...");
    UserDAO userDAO = new MySqlUserDAO();
    ItemDAO itemDAO = new MySqlItemDAO();
    AuctionDAO auctionDAO = new MySqlAuctionDAO();
    AutoBidDAO autoBidDAO = new MySqlAutoBidDAO();
    BidDAO bidDAO = new MySqlBidDAO();
    TransactionManager transactionManager = new TransactionManager();
    BidValidator bidValidator = new BidValidator();
    AntiSnipeService antiSnipeService = new AntiSnipeService();
    UserService userService = new UserService(userDAO, transactionManager);
    final ItemService itemService = new ItemService(itemDAO, transactionManager);
    final BidService bidService =
        new BidService(
            bidDAO, auctionDAO, userDAO, transactionManager, bidValidator, antiSnipeService);
    final AuctionService auctionService =
        new AuctionService(auctionDAO, bidDAO, itemDAO, userDAO, transactionManager);
    User seller;
    User buyer1;
    User buyer2;
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
    if (seller == null || buyer1 == null || buyer2 == null) {
      System.out.println("Loi tao DB User! Vui long kiem tra MySQL.");
      return;
    }
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
    System.out.println("\n3. Mo phien dau gia (Ket thuc sau 5 giay)...");
    Auction auction =
        new Auction(
            phone.getId(),
            seller.getId(),
            LocalDateTime.now().plusSeconds(5),
            phone.getStartingPrice());
    auction = auctionService.createAuction(auction);
    System.out.println("Phien dau gia tao voi ID: " + auction.getId());
    auctionService.updateStatus(auction.getId(), AuctionStatus.RUNNING);
    auction.start();
    auctionService.setStartTime(auction.getId(), LocalDateTime.now());
    phone.setStatus(ItemStatus.UNDER_AUCTION);
    itemService.updateStatus(phone.getId(), ItemStatus.UNDER_AUCTION);
    System.out.println("\n4. Nguoi mua bat dau dat gia...");
    bidService.placeBid(auction.getId(), buyer1.getId(), 1050);
    bidService.placeBid(auction.getId(), buyer2.getId(), 1200);
    bidService.placeBid(auction.getId(), buyer1.getId(), 1300);
    System.out.println("\n6. Cho 5.5 giay de phien het han...");
    try {
      auctionService.setEndTime(auction.getId(), LocalDateTime.now().plusSeconds(5));
      Thread.sleep(5500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }
    auctionService.handleCompletion(auction.getId());
    phone.setStatus(ItemStatus.SOLD);
    itemService.updateStatus(phone.getId(), ItemStatus.SOLD);
  }
}
