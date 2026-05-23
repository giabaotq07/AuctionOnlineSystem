package app.server.service;

import static org.junit.jupiter.api.Assertions.*;

import app.TestFixtures;
import app.common.dto.*;
import app.common.enums.*;
import app.common.exception.ServiceException;
import app.common.models.*;
import app.server.dao.*;
import app.server.dao.impl.*;
import app.server.database.TransactionManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Bo sung integration tests cho toan bo cac Service trong module server. Chay tich hop tren H2
 * Database va dam bao do phu tren 85%. Viet bang tieng Viet khong dau theo dung yeu cau.
 */
public class AdditionalServicesTest extends BaseDAOTest {
  private UserDAO userDAO;
  private ItemDAO itemDAO;
  private AuctionDAO auctionDAO;
  private BidDAO bidDAO;
  private AutoBidDAO autoBidDAO;
  private TransactionManager transactionManager;

  private AuctionQueryService queryService;
  private ItemService itemService;
  private BidService bidService;
  private AutoBidService autoBidService;
  private BidValidator bidValidator;
  private ImageStorageService imageStorageService;
  private AuctionService auctionService;

  private User seller;
  private User bidder;
  private User admin;
  private Item item;
  private Auction auction;

  @BeforeEach
  void setUp() {
    userDAO = new MySqlUserDAO();
    itemDAO = new MySqlItemDAO();
    auctionDAO = new MySqlAuctionDAO();
    bidDAO = new MySqlBidDAO();
    autoBidDAO = new MySqlAutoBidDAO();
    transactionManager = new TransactionManager();

    bidValidator = new BidValidator();
    imageStorageService = new ImageStorageService();
    queryService = new AuctionQueryService(auctionDAO, bidDAO, itemDAO, userDAO);
    itemService = new ItemService(itemDAO, auctionDAO, transactionManager);

    AntiSnipeService antiSnipeService = new AntiSnipeService();
    autoBidService =
        new AutoBidService(
            autoBidDAO,
            auctionDAO,
            bidDAO,
            itemDAO,
            userDAO,
            transactionManager,
            bidValidator,
            antiSnipeService);
    bidService =
        new BidService(
            bidDAO,
            auctionDAO,
            itemDAO,
            userDAO,
            transactionManager,
            bidValidator,
            antiSnipeService,
            autoBidService);

    AuctionSettlementService settlementService = new AuctionSettlementService(bidDAO, userDAO);
    auctionService =
        new AuctionService(
            auctionDAO,
            bidDAO,
            itemDAO,
            userDAO,
            transactionManager,
            settlementService,
            Clock.systemDefaultZone());

    // Tao du lieu mau phuc vu testcase
    seller =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("seller"), UserRole.SELLER, BigDecimal.valueOf(1000)));
    bidder =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidder"), UserRole.BIDDER, BigDecimal.valueOf(5000)));
    admin =
        userDAO.save(
            TestFixtures.user(TestFixtures.unique("admin"), UserRole.ADMIN, BigDecimal.ZERO));

    // De H2 tu dong tang va quan ly ID cua Item nham tranh loi khoa ngoai (Foreign Key Violation)
    item = itemDAO.save(TestFixtures.item(seller.getId(), "Mona Lisa Copy", ItemType.ART));

    auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusDays(1), 1000L));
  }

  // ==========================================
  // 1. TEST BID VALIDATOR
  // ==========================================
  @Test
  public void testBidValidator() {
    Auction auc = new Auction(1, 1, LocalDateTime.now().plusDays(1), 100L);
    auc.setStatus(AuctionStatus.OPEN);

    // ValidateState nem loi khi status != RUNNING
    assertThrows(ServiceException.class, () -> bidValidator.validateAuctionState(auc));

    auc.start();
    assertDoesNotThrow(() -> bidValidator.validateAuctionState(auc));

    // ValidateBidAmount
    assertThrows(ServiceException.class, () -> bidValidator.validateBidAmount(120L, 100L, -5L));
    assertThrows(ServiceException.class, () -> bidValidator.validateBidAmount(105L, 100L, 10L));
    assertDoesNotThrow(() -> bidValidator.validateBidAmount(110L, 100L, 10L));
  }

  // ==========================================
  // 2. TEST IMAGE STORAGE SERVICE
  // ==========================================
  @Test
  public void testImageStorageService() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> imageStorageService.save(null, "img.jpg"));
    assertThrows(IllegalArgumentException.class, () -> imageStorageService.save("   ", "img.jpg"));
    assertThrows(
        IllegalArgumentException.class,
        () -> imageStorageService.save("invalid_base64!", "img.jpg"));

    // Test file qua lon (6MB)
    byte[] largeBytes = new byte[6 * 1024 * 1024];
    String largeBase64 = java.util.Base64.getEncoder().encodeToString(largeBytes);
    assertThrows(
        IllegalArgumentException.class, () -> imageStorageService.save(largeBase64, "img.jpg"));

    // Test save thanh cong
    String validBase64 =
        java.util.Base64.getEncoder().encodeToString("dummy_image_data".getBytes());
    String relativePath = imageStorageService.save(validBase64, "avatar.png");

    assertNotNull(relativePath);
    assertTrue(relativePath.startsWith("server_data/images/"));
    assertTrue(relativePath.endsWith(".png"));

    // Doc nguoc lai anh
    String readBase64 = imageStorageService.readAsBase64(relativePath);
    assertEquals(validBase64, readBase64);

    // Xoa anh
    imageStorageService.deleteIfExists(relativePath);
    assertThrows(Exception.class, () -> imageStorageService.readAsBase64(relativePath));

    // Xoa an toan du duong dan null hoac khong ton tai
    assertDoesNotThrow(() -> imageStorageService.deleteIfExists(null));
    assertDoesNotThrow(
        () -> imageStorageService.deleteIfExists("server_data/images/nonexistent.jpg"));

    assertThrows(IllegalArgumentException.class, () -> imageStorageService.readAsBase64(null));
  }

  // ==========================================
  // 3. TEST AUCTION QUERY SERVICE
  // ==========================================
  @Test
  public void testAuctionQueryService() {
    List<Auction> list = queryService.getAuctions();
    assertFalse(list.isEmpty());

    List<AuctionPreview> previews = queryService.getAuctionPreviews();
    assertFalse(previews.isEmpty());

    Auction found = queryService.getAuction(auction.getId());
    assertNotNull(found);
    assertEquals(auction.getId(), found.getId());

    Auction detail = queryService.getAuctionDetail(auction.getId());
    assertNotNull(detail);

    assertThrows(ServiceException.class, () -> queryService.getAuction(-999));

    assertTrue(queryService.isAuctionVersionCurrent(auction.getId(), auction.getVersion()));
    assertFalse(queryService.isAuctionVersionCurrent(auction.getId(), -1));

    // Test history
    auction.setStatus(AuctionStatus.FINISHED);
    auctionDAO.update(auction);
    List<Auction> history = queryService.getHistoryAuctions(seller.getId());
    assertFalse(history.isEmpty());

    List<AuctionPreview> historyPreviews = queryService.getHistoryAuctionPreviews(seller.getId());
    assertFalse(historyPreviews.isEmpty());

    List<Auction> byItem = queryService.getAuctionsByItem(item.getId());
    assertFalse(byItem.isEmpty());
  }

  // ==========================================
  // 4. TEST BID SERVICE
  // ==========================================
  @Test
  public void testBidService() {
    // Validate inputs
    assertThrows(ServiceException.class, () -> bidService.placeBid(auction.getId(), null, 1500L));
    assertThrows(ServiceException.class, () -> bidService.placeBid(auction.getId(), seller, 1500L));
    assertThrows(ServiceException.class, () -> bidService.placeBid(-5, bidder, 1500L));
    assertThrows(ServiceException.class, () -> bidService.placeBid(auction.getId(), bidder, -10L));

    // Seller khong duoc bid san pham cua minh
    User sellerBidder = userDAO.save(TestFixtures.user("seller_bidder", UserRole.BIDDER));

    // Tao mot Item moi cho sellerBidder nham thoa man rang buoc UNIQUE(item_id) cua
    // auction_sessions
    Item sellerItem =
        itemDAO.save(TestFixtures.item(sellerBidder.getId(), "Another Vase", ItemType.ART));

    Auction sellerAuc =
        auctionDAO.save(
            TestFixtures.auction(
                sellerItem.getId(), sellerBidder.getId(), LocalDateTime.now().plusDays(1), 1000L));
    assertThrows(
        ServiceException.class, () -> bidService.placeBid(sellerAuc.getId(), sellerBidder, 1500L));

    // Phien khong RUNNING
    assertThrows(ServiceException.class, () -> bidService.placeBid(auction.getId(), bidder, 1500L));

    // Bat dau phien
    auction.start();
    auctionDAO.update(auction);

    assertThrows(ServiceException.class, () -> bidService.placeBid(auction.getId(), bidder, -10L));
    assertThrows(ServiceException.class, () -> bidService.placeBid(auction.getId(), bidder, 1050L));

    // Bid thanh cong
    Auction bidded = bidService.placeBid(auction.getId(), bidder, 1500L);
    assertNotNull(bidded);
    assertEquals(1500L, bidded.getHighestBid());
    assertEquals(bidder.getId(), bidded.getWinnerId());
  }

  // ==========================================
  // 5. TEST ITEM SERVICE
  // ==========================================
  @Test
  public void testItemService() {
    Item item2 =
        ItemFactory.createItem(
            "Phone X", seller.getId(), "Modern phone", 500L, 50L, ItemType.ELECTRONICS);
    Item saved = itemService.add(item2);
    assertNotNull(saved);
    assertTrue(saved.getId() > 0);

    Optional<Item> found = itemService.getById(saved.getId());
    assertTrue(found.isPresent());

    saved.setName("Phone X Pro");
    itemService.update(saved);
    assertEquals("Phone X Pro", itemService.getById(saved.getId()).get().getName());

    List<Item> sellerItems =
        itemService.getSellerItems(seller.getId(), UserRole.SELLER, seller.getId());
    assertFalse(sellerItems.isEmpty());

    // Quyen xem seller items
    assertThrows(
        ServiceException.class,
        () -> itemService.getSellerItems(bidder.getId(), UserRole.BIDDER, seller.getId()));

    // updateManagedItem validations
    assertThrows(
        ServiceException.class, () -> itemService.updateManagedItem(saved, -1, UserRole.SELLER));

    // updateManagedItem thanh cong
    Item updated = itemService.updateManagedItem(saved, seller.getId(), UserRole.SELLER);
    assertEquals("Phone X Pro", updated.getName());

    // updateImagePath
    String oldPath =
        itemService.updateImagePath(
            saved.getId(), "server_data/images/new.jpg", seller.getId(), UserRole.SELLER);
    assertEquals(
        "server_data/images/new.jpg", itemService.getById(saved.getId()).get().getImageUrl());

    // softDelete
    itemService.softDeleteManagedItem(saved.getId(), seller.getId(), UserRole.SELLER);
    assertTrue(itemService.getById(saved.getId()).get().isDeleted());

    // delete
    itemService.delete(saved.getId());
    assertTrue(itemService.getById(saved.getId()).get().isDeleted());

    // getAll
    assertFalse(itemService.getAll().isEmpty());
  }

  // ==========================================
  // 6. TEST AUCTION SCHEDULER
  // ==========================================
  @Test
  public void testAuctionScheduler() {
    AuctionScheduler scheduler = AuctionScheduler.getInstance();
    assertNotNull(scheduler);

    // Init scheduler
    assertDoesNotThrow(() -> scheduler.init(auctionService, queryService));

    // Schedule start
    assertDoesNotThrow(
        () -> scheduler.scheduleStart(auction.getId(), LocalDateTime.now().minusSeconds(1)));

    // Shutdown
    assertDoesNotThrow(() -> scheduler.shutdown());
  }
}
