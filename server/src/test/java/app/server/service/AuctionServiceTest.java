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

  /** Test tao phien that bai khi du lieu khong hop le. */
  @Test
  public void testCreateAuctionInvalidPayload() {
    LocalDateTime startTime = LocalDateTime.now().plusHours(1);

    assertThrows(
        ServiceException.class,
        () ->
            auctionService.createAuction(
                " ", "Mo ta", 1000L, 100L, ItemType.ART, 60, startTime, seller));
    assertThrows(
        ServiceException.class,
        () ->
            auctionService.createAuction(
                "San pham", "Mo ta", 0L, 100L, ItemType.ART, 60, startTime, seller));
    assertThrows(
        ServiceException.class,
        () ->
            auctionService.createAuction(
                "San pham", "Mo ta", 1000L, 0L, ItemType.ART, 60, startTime, seller));
    assertThrows(
        ServiceException.class,
        () ->
            auctionService.createAuction(
                "San pham", "Mo ta", 1000L, 100L, ItemType.ART, 0, startTime, seller));
    assertThrows(
        ServiceException.class,
        () ->
            auctionService.createAuction(
                "San pham", "Mo ta", 1000L, 100L, ItemType.ART, 60, null, seller));
    assertThrows(
        ServiceException.class,
        () ->
            auctionService.createAuction(
                "San pham",
                "Mo ta",
                1000L,
                100L,
                ItemType.ART,
                30,
                LocalDateTime.now().minusHours(2),
                seller));
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

  /** Test cap nhat phien that bai neu khong phai chu phien hoac admin. */
  @Test
  public void testUpdateAuctionNoPermission() {
    User otherSeller =
        userDAO.save(TestFixtures.user("other_seller", UserRole.SELLER, new BigDecimal("1000")));
    LocalDateTime startTime = LocalDateTime.now().plusHours(2);
    Auction auction =
        auctionService.createAuction(
            "Dong ho co", "Dong ho suu tam", 12000L, 1000L, ItemType.ART, 60, startTime, seller);

    assertThrows(
        ServiceException.class,
        () ->
            auctionService.updateAuction(
                auction.getId(),
                "Dong ho moi",
                "Mo ta moi",
                13000L,
                1000L,
                ItemType.ART,
                90,
                startTime.plusHours(1),
                auction.getVersion(),
                otherSeller));
  }

  /** Test khong duoc cap nhat phien dang chay. */
  @Test
  public void testUpdateRunningAuctionRejected() {
    LocalDateTime startTime = LocalDateTime.now().minusMinutes(5);
    Auction auction =
        auctionService.createAuction(
            "May anh", "May anh phim", 9000L, 500L, ItemType.ELECTRONICS, 60, startTime, seller);

    assertThrows(
        ServiceException.class,
        () ->
            auctionService.updateAuction(
                auction.getId(),
                "May anh moi",
                "Mo ta moi",
                9500L,
                500L,
                ItemType.ELECTRONICS,
                60,
                LocalDateTime.now().plusHours(1),
                auction.getVersion(),
                seller));
  }

  /** Test uu tien loi trang thai khi cap nhat phien da dong. */
  @Test
  public void testUpdateFinishedAuctionReturnsClosedMessageBeforePayloadValidation() {
    LocalDateTime startTime = LocalDateTime.now().plusHours(2);
    Auction auction =
        auctionService.createAuction(
            "Binh gom", "Binh gom co", 7000L, 500L, ItemType.ART, 60, startTime, seller);
    auction.setStatus(AuctionStatus.FINISHED);
    auctionDAO.update(auction);

    ServiceException ex =
        assertThrows(
            ServiceException.class,
            () ->
                auctionService.updateAuction(
                    auction.getId(),
                    "Binh gom moi",
                    "Mo ta moi",
                    8000L,
                    500L,
                    ItemType.ART,
                    30,
                    LocalDateTime.now().minusHours(2),
                    auction.getVersion(),
                    admin));
    assertEquals("Không thể cập nhật phiên đã đóng.", ex.getMessage());
  }

  /** Test khong duoc huy phien da ket thuc. */
  @Test
  public void testCancelFinishedAuctionRejected() {
    LocalDateTime startTime = LocalDateTime.now().plusHours(2);
    Auction auction =
        auctionService.createAuction(
            "Tuong go", "Do go my nghe", 7000L, 500L, ItemType.ART, 60, startTime, seller);
    auction.setStatus(AuctionStatus.FINISHED);
    auctionDAO.update(auction);

    assertThrows(
        ServiceException.class,
        () -> auctionService.cancelAuction(auction.getId(), seller, auction.getVersion()));
  }

  /** Test startOpenAuction - chuyen trang thai OPEN sang RUNNING. */
  @Test
  public void testStartOpenAuction() {
    LocalDateTime startTime = LocalDateTime.now().plusHours(2);
    Auction auction =
        auctionService.createAuction(
            "San pham A", "Mo ta", 5000L, 500L, ItemType.ART, 60, startTime, seller);

    // Dat startTime ve qua khu de startOpenAuction co the chay
    auction.setStartTime(LocalDateTime.now().minusMinutes(1));
    auctionDAO.update(auction);

    boolean started = auctionService.startOpenAuction(auction.getId());
    assertTrue(started);

    Auction stored = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(AuctionStatus.RUNNING, stored.getStatus());
  }

  /** Test startOpenAuction khi phien khong OPEN - phai tra ve false. */
  @Test
  public void testStartOpenAuction_skipsNonOpen() {
    LocalDateTime startTime = LocalDateTime.now().minusMinutes(10);
    Auction auction =
        auctionService.createAuction(
            "San pham B", "Mo ta", 5000L, 500L, ItemType.ART, 120, startTime, seller);
    // Phien nay da RUNNING khi tao (startTime trong qua khu)
    assertEquals(AuctionStatus.RUNNING, auction.getStatus());

    // startOpenAuction voi phien RUNNING phai tra ve false
    boolean started = auctionService.startOpenAuction(auction.getId());
    assertFalse(started);
  }

  /** Test completeAuction - phien het han co bidder thang cuoc. */
  @Test
  public void testCompleteAuction_withWinner() {
    // Tao phien voi duration 180 phut: endTime = now - 2h + 3h = now + 1h (pass validation)
    LocalDateTime pastStart = LocalDateTime.now().minusHours(2);
    Auction auction =
        auctionService.createAuction(
            "San pham C", "Mo ta", 3000L, 300L, ItemType.ELECTRONICS, 180, pastStart, seller);
    // Phien da RUNNING vi startTime la trong qua khu
    assertEquals(AuctionStatus.RUNNING, auction.getStatus());

    // Set endTime ve qua khu de phien xem la het han
    transactionManager.runWithoutResult(
        conn -> {
          Auction locked = auctionDAO.findById(conn, auction.getId()).orElseThrow();
          locked.setEndTime(LocalDateTime.now().minusMinutes(5));
          auctionDAO.update(conn, locked);
        });

    // Deposit du tien cho bidder truoc khi dat gia
    bidder = userDAO.findById(bidder.getId()).orElseThrow();
    bidder.getWallet().deposit(new BigDecimal("10000"));
    userDAO.update(bidder);
    bidder = userDAO.findById(bidder.getId()).orElseThrow();
    bidder.getWallet().setFrozenAmount(String.valueOf(auction.getId()), new BigDecimal("3500"));
    userDAO.update(bidder);
    transactionManager.runWithoutResult(
        conn -> bidDAO.insertBid(conn, auction.getId(), bidder.getId(), 3500L, false));

    var completion = auctionService.completeAuction(auction.getId());

    assertTrue(completion.completed());
    assertEquals(auction.getId(), completion.auctionId());
    assertTrue(completion.highestBid().isPresent());
  }

  /** Test completeAuction - phien het han khong co bidder. */
  @Test
  public void testCompleteAuction_noWinner() {
    // Duration 180 phut: endTime = now - 2h + 3h = now + 1h (pass validation)
    LocalDateTime pastStart = LocalDateTime.now().minusHours(2);
    Auction auction =
        auctionService.createAuction(
            "San pham D", "Mo ta", 3000L, 300L, ItemType.ELECTRONICS, 180, pastStart, seller);

    // Set endTime ve qua khu de phien xem la het han
    transactionManager.runWithoutResult(
        conn -> {
          Auction locked = auctionDAO.findById(conn, auction.getId()).orElseThrow();
          locked.setEndTime(LocalDateTime.now().minusMinutes(5));
          auctionDAO.update(conn, locked);
        });

    var completion = auctionService.completeAuction(auction.getId());

    assertTrue(completion.completed());
    assertFalse(completion.highestBid().isPresent());
  }

  /** Test completeAuction - phien chua het han, phai tra ve false. */
  @Test
  public void testCompleteAuction_notExpiredYet() {
    LocalDateTime startTime = LocalDateTime.now().minusMinutes(10);
    Auction auction =
        auctionService.createAuction(
            "San pham E", "Mo ta", 5000L, 500L, ItemType.ART, 120, startTime, seller);

    var completion = auctionService.completeAuction(auction.getId());
    assertFalse(completion.completed());
  }

  /** Test settleAuctionPayment - phien FINISHED co winner. */
  @Test
  public void testSettleAuctionPayment() {
    LocalDateTime startTime = LocalDateTime.now().plusHours(2);
    Auction auction =
        auctionService.createAuction(
            "San pham F", "Mo ta", 5000L, 500L, ItemType.ART, 60, startTime, seller);

    // Chuyen phien sang FINISHED voi winner
    auction.setStatus(AuctionStatus.FINISHED);
    auction.updateHighestBid(5500L, bidder.getId());
    auctionDAO.update(auction);

    // Nap tien vao vi bidder truoc khi freeze
    bidder = userDAO.findById(bidder.getId()).orElseThrow();
    bidder.getWallet().deposit(new BigDecimal("10000"));
    userDAO.update(bidder);
    // Set frozen amount (bidder da co du so du)
    bidder = userDAO.findById(bidder.getId()).orElseThrow();
    bidder.getWallet().setFrozenAmount(String.valueOf(auction.getId()), new BigDecimal("5500"));
    userDAO.update(bidder);
    transactionManager.runWithoutResult(
        conn -> bidDAO.insertBid(conn, auction.getId(), bidder.getId(), 5500L, false));

    var amount = auctionService.settleAuctionPayment(auction.getId());

    assertTrue(amount.compareTo(BigDecimal.ZERO) > 0);

    // Kiem tra phien duoc chuyen sang PAID
    Auction stored = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(AuctionStatus.PAID, stored.getStatus());
  }

  /** Test settleAuctionPayment khi phien da o trang thai PAID. */
  @Test
  public void testSettleAuctionPayment_alreadyPaid() {
    LocalDateTime startTime = LocalDateTime.now().plusHours(2);
    Auction auction =
        auctionService.createAuction(
            "San pham G", "Mo ta", 5000L, 500L, ItemType.ART, 60, startTime, seller);

    auction.setStatus(AuctionStatus.PAID);
    auctionDAO.update(auction);

    var amount = auctionService.settleAuctionPayment(auction.getId());
    assertEquals(BigDecimal.ZERO, amount);
  }

  /** Test completeExpiredAuctionCompletions - batch ket thuc phien het han. */
  @Test
  public void testCompleteExpiredAuctionCompletions() {
    // Duration 185 phut: endTime = now - 3h + 3h5m = now + 5min (pass validation)
    LocalDateTime pastStart = LocalDateTime.now().minusHours(3);
    Auction expired =
        auctionService.createAuction(
            "Phien het han", "Mo ta", 2000L, 200L, ItemType.ELECTRONICS, 185, pastStart, seller);
    // Set endTime ve qua khu de batch completion nhan ra
    transactionManager.runWithoutResult(
        conn -> {
          Auction locked = auctionDAO.findById(conn, expired.getId()).orElseThrow();
          locked.setEndTime(LocalDateTime.now().minusMinutes(1));
          auctionDAO.update(conn, locked);
        });

    // Tao 1 phien chua het han
    LocalDateTime futureStart = LocalDateTime.now().plusHours(1);
    auctionService.createAuction(
        "Phien con han", "Mo ta", 3000L, 300L, ItemType.ART, 120, futureStart, seller);

    var completions = auctionService.completeExpiredAuctionCompletions();
    assertFalse(completions.isEmpty());
  }

  /** Test validateAuctionIdentity - auctionId hoac expectedVersion khong hop le. */
  @Test
  public void testValidateAuctionIdentity_invalid() {
    assertThrows(
        app.common.exception.ServiceException.class,
        () -> auctionService.cancelAuction(0, seller, 0));
    assertThrows(
        app.common.exception.ServiceException.class,
        () -> auctionService.cancelAuction(1, seller, -1));
  }

  /** Test updateAuction - thay doi startingPrice lam thay doi highestBid. */
  @Test
  public void testUpdateAuction_success() {
    LocalDateTime startTime = LocalDateTime.now().plusHours(2);
    Auction auction =
        auctionService.createAuction(
            "Hang hoa XYZ",
            "Mo ta ban dau",
            8000L,
            800L,
            ItemType.ELECTRONICS,
            60,
            startTime,
            seller);

    Auction updated =
        auctionService.updateAuction(
            auction.getId(),
            "Hang hoa XYZ v2",
            "Mo ta moi",
            9000L,
            900L,
            ItemType.ELECTRONICS,
            90,
            startTime.plusHours(1),
            auction.getVersion(),
            seller);

    assertNotNull(updated);
    assertEquals(9000L, updated.getHighestBid());
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
