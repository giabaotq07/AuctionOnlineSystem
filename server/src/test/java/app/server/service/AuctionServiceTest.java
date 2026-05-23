package app.server.service;

import static org.junit.jupiter.api.Assertions.*;

import app.TestFixtures;
import app.common.enums.AuctionStatus;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.models.*;
import app.server.dao.*;
import app.server.dao.impl.*;
import app.server.database.TransactionManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Lop kiem thu cho AuctionService. Viet bang tieng Viet khong dau de giai thich cho mentor. */
public class AuctionServiceTest extends BaseDAOTest {
  private static final Logger logger = LoggerFactory.getLogger(AuctionServiceTest.class);

  private AuctionService auctionService;
  private AuctionDAO auctionDAO;
  private BidDAO bidDAO;
  private ItemDAO itemDAO;
  private UserDAO userDAO;
  private TransactionManager transactionManager;

  private User seller;
  private User bidder;
  private User admin;

  /** Thiet lap moi truong database va khoi tao cac service, dao can thiet. */
  @BeforeEach
  public void setUp() {
    logger.info("Thiet lap moi truong test cho AuctionService...");
    auctionDAO = new MySqlAuctionDAO();
    bidDAO = new MySqlBidDAO();
    itemDAO = new MySqlItemDAO();
    userDAO = new MySqlUserDAO();
    transactionManager = new TransactionManager();
    AuctionSettlementService settlementService = new AuctionSettlementService(bidDAO, userDAO);
    Clock clock = Clock.systemDefaultZone();

    auctionService =
        new AuctionService(
            auctionDAO, bidDAO, itemDAO, userDAO, transactionManager, settlementService, clock);

    // Xoa du lieu cu
    cleanAllData();

    // Tao du lieu nguoi dung phuc vu testcase
    seller = TestFixtures.user("seller_user", UserRole.SELLER, new BigDecimal("1000"));
    bidder = TestFixtures.user("bidder_user", UserRole.BIDDER, new BigDecimal("5000"));
    admin = TestFixtures.user("admin_user", UserRole.ADMIN, new BigDecimal("0"));

    seller = userDAO.save(seller);
    bidder = userDAO.save(bidder);
    admin = userDAO.save(admin);
  }

  /** Test tao phien dau gia thanh cong. */
  @Test
  public void testCreateAuctionSuccess() {
    LocalDateTime startTime = LocalDateTime.now().plusHours(1);
    Auction auction =
        auctionService.createAuction(
            "Buc hoa Mona Lisa",
            "Tranh son dau noi tieng",
            10000L,
            1000L,
            ItemType.ART,
            60,
            startTime,
            seller);

    assertNotNull(auction);
    assertTrue(auction.getId() > 0);
    assertEquals(AuctionStatus.OPEN, auction.getStatus());
    assertEquals(10000L, auction.getHighestBid());

    // Kiem tra du lieu trong DB
    transactionManager.runWithoutResult(
        conn -> {
          Auction stored = auctionDAO.findById(conn, auction.getId()).orElse(null);
          assertNotNull(stored);
          assertEquals(seller.getId(), stored.getSellerId());
          assertEquals(AuctionStatus.OPEN, stored.getStatus());
        });
  }

  /** Test tao phien dau gia bat dau ngay lập tức. */
  @Test
  public void testCreateAuctionStartImmediately() {
    LocalDateTime startTime = LocalDateTime.now().minusMinutes(5); // Da bat dau tu 5 phut truoc
    Auction auction =
        auctionService.createAuction(
            "iPhone 15 Pro Max",
            "Dien thoai moi 99%",
            20000L,
            2000L,
            ItemType.ELECTRONICS,
            120,
            startTime,
            seller);

    assertNotNull(auction);
    // Vi start time o qua khu, phien phai tu dong chuyen sang RUNNING
    assertEquals(AuctionStatus.RUNNING, auction.getStatus());
  }

  /** Test huy phien dau gia boi chu so huu. */
  @Test
  public void testCancelAuctionByOwner() {
    LocalDateTime startTime = LocalDateTime.now().plusHours(2);
    Auction auction =
        auctionService.createAuction(
            "Xe co Vespa 1970",
            "Xe con chay tot",
            50000L,
            5000L,
            ItemType.VEHICLE,
            180,
            startTime,
            seller);

    // Owner huy phien dang OPEN
    Set<Integer> affectedUsers =
        auctionService.cancelAuction(auction.getId(), seller, auction.getVersion());
    assertNotNull(affectedUsers);

    transactionManager.runWithoutResult(
        conn -> {
          Auction stored = auctionDAO.findById(conn, auction.getId()).orElse(null);
          assertNotNull(stored);
          assertEquals(AuctionStatus.CANCELED, stored.getStatus());
        });
  }

  /** Test Admin huy phien dang chay (RUNNING). */
  @Test
  public void testCancelRunningAuctionByAdmin() {
    LocalDateTime startTime = LocalDateTime.now().minusMinutes(10);
    Auction auction =
        auctionService.createAuction(
            "Laptop Dell XPS",
            "Laptop van phong",
            15000L,
            1000L,
            ItemType.ELECTRONICS,
            30,
            startTime,
            seller);

    assertEquals(AuctionStatus.RUNNING, auction.getStatus());

    // Admin huy phien RUNNING
    Set<Integer> affectedUsers =
        auctionService.cancelAuction(auction.getId(), admin, auction.getVersion());
    assertNotNull(affectedUsers);

    transactionManager.runWithoutResult(
        conn -> {
          Auction stored = auctionDAO.findById(conn, auction.getId()).orElse(null);
          assertNotNull(stored);
          assertEquals(AuctionStatus.CANCELED, stored.getStatus());
        });
  }

  /** Test huy phien that bai neu khong phai Owner hoac Admin. */
  @Test
  public void testCancelAuctionNoPermission() {
    LocalDateTime startTime = LocalDateTime.now().plusHours(2);
    Auction auction =
        auctionService.createAuction(
            "Tranh thuy mac", "Tranh nghe thuat", 8000L, 500L, ItemType.ART, 60, startTime, seller);

    // Bidder khong co quyen huy phien OPEN cua Seller
    assertThrows(
        ServiceException.class,
        () -> {
          auctionService.cancelAuction(auction.getId(), bidder, auction.getVersion());
        });
  }

  /** Kiet tac clean du lieu de tranh anh huong giua cac testcase. */
  private void cleanAllData() {
    transactionManager.runWithoutResult(
        conn -> {
          try (var stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM auto_bids");
            stmt.executeUpdate("DELETE FROM bids");
            stmt.executeUpdate("DELETE FROM auction_sessions");
            stmt.executeUpdate("DELETE FROM items");
            stmt.executeUpdate("DELETE FROM users");
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }
}
