package app.server.service;

import static org.junit.jupiter.api.Assertions.*;

import app.TestFixtures;
import app.common.enums.AuctionStatus;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.models.Auction;
import app.common.models.Item;
import app.common.models.User;
import app.server.dao.AuctionDAO;
import app.server.dao.BaseDAOTest;
import app.server.dao.BidDAO;
import app.server.dao.ItemDAO;
import app.server.dao.UserDAO;
import app.server.dao.impl.MySqlAuctionDAO;
import app.server.dao.impl.MySqlBidDAO;
import app.server.dao.impl.MySqlItemDAO;
import app.server.dao.impl.MySqlUserDAO;
import app.server.database.TransactionManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuctionServiceTest extends BaseDAOTest {
  private UserDAO userDAO;
  private ItemDAO itemDAO;
  private AuctionDAO auctionDAO;
  private BidDAO bidDAO;
  private AuctionService auctionService;
  private TransactionManager transactionManager;
  private User seller;

  @BeforeEach
  void setUp() {
    userDAO = new MySqlUserDAO();
    itemDAO = new MySqlItemDAO();
    auctionDAO = new MySqlAuctionDAO();
    bidDAO = new MySqlBidDAO();
    transactionManager = new TransactionManager();
    auctionService = new AuctionService(auctionDAO, bidDAO, itemDAO, userDAO, transactionManager);
    seller = userDAO.save(TestFixtures.user(TestFixtures.unique("seller"), UserRole.SELLER));
  }

  @Test
  void createAuction_shouldPersistFutureAuction() {
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));

    Auction saved =
        auctionService.createAuction(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));

    Auction found = auctionDAO.findById(saved.getId()).orElseThrow();
    assertEquals(AuctionStatus.OPEN, found.getStatus());
    assertEquals(1000L, found.getHighestBid());
  }

  @Test
  void createAuction_shouldRejectPastEndTime() {
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));
    Auction auction =
        TestFixtures.auction(
            item.getId(), seller.getId(), LocalDateTime.now().minusMinutes(1), 1000L);

    assertThrows(ServiceException.class, () -> auctionService.createAuction(auction));
  }

  @Test
  void createAndStartAuction_shouldPersistRunningAuction() {
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Camera", ItemType.ELECTRONICS));

    Auction saved = auctionService.createAndStartAuction(item.getId(), seller.getId(), 1000L, 10);

    Auction found = auctionDAO.findById(saved.getId()).orElseThrow();
    assertEquals(AuctionStatus.RUNNING, found.getStatus());
    assertNotNull(found.getStartTime());
  }

  @Test
  void createAndStartAuctionWithItem_shouldPersistItemAndRunningAuction() {
    Auction created =
        auctionService.createAndStartAuctionWithItem(
            "Camera",
            "Test camera",
            1000L,
            100L,
            ItemType.ELECTRONICS,
            10,
            seller.getId(),
            seller.getRole());

    Auction found = auctionDAO.findById(created.getId()).orElseThrow();
    Item item = itemDAO.findById(found.getItemId()).orElseThrow();
    assertEquals(AuctionStatus.RUNNING, found.getStatus());
    assertEquals("Camera", item.getName());
    assertEquals(seller.getId(), item.getSellerId());
  }

  @Test
  void completeAndGetHighestBid_shouldReturnHighestBidWhenBidsExist() {
    User bidder = userDAO.save(TestFixtures.user(TestFixtures.unique("bidder"), UserRole.BIDDER));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Laptop", ItemType.ELECTRONICS));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    bidDAO.insertBid(auction.getId(), bidder.getId(), 1500L, false);

    var highestBid = auctionService.completeAndGetHighestBid(auction.getId()).orElseThrow();

    assertEquals(auction.getId(), highestBid.getAuctionId());
    assertEquals(bidder.getId(), highestBid.getBidderId());
    assertEquals(1500L, highestBid.getAmount());
  }

  @Test
  void handleCompletion_shouldFinishExpiredAuctionAndSetWinner() {
    User bidder = userDAO.save(TestFixtures.user(TestFixtures.unique("bidder"), UserRole.BIDDER));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Tablet", ItemType.ELECTRONICS));
    Auction auction =
        TestFixtures.auction(
            item.getId(), seller.getId(), LocalDateTime.now().minusMinutes(1), 1000L);
    auction.start();
    auction = auctionDAO.save(auction);
    bidDAO.insertBid(auction.getId(), bidder.getId(), 1500L, false);

    auctionService.handleCompletion(auction.getId());

    Auction finished = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(AuctionStatus.FINISHED, finished.getStatus());
    assertEquals(bidder.getId(), finished.getWinnerId());
  }

  @Test
  void cancelAuction_shouldAllowSellerOwner() {
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Speaker", ItemType.ELECTRONICS));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));

    auctionService.cancelAuction(auction.getId(), seller.getId(), auction.getVersion());

    Auction found = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(AuctionStatus.CANCELED, found.getStatus());
  }

  @Test
  void cancelAuction_shouldAllowAdmin() {
    User admin = userDAO.save(TestFixtures.user(TestFixtures.unique("admin"), UserRole.ADMIN));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Speaker", ItemType.ELECTRONICS));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));

    auctionService.cancelAuction(auction.getId(), admin.getId(), auction.getVersion());

    Auction found = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(AuctionStatus.CANCELED, found.getStatus());
  }

  @Test
  void cancelAuction_shouldRejectNonOwnerNonAdmin() {
    User otherSeller =
        userDAO.save(TestFixtures.user(TestFixtures.unique("other_seller"), UserRole.SELLER));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Speaker", ItemType.ELECTRONICS));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));

    assertThrows(
        ServiceException.class,
        () ->
            auctionService.cancelAuction(
                auction.getId(), otherSeller.getId(), auction.getVersion()));

    Auction found = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(AuctionStatus.OPEN, found.getStatus());
  }

  @Test
  void cancelAuction_shouldRejectStaleVersion() {
    User admin = userDAO.save(TestFixtures.user(TestFixtures.unique("admin"), UserRole.ADMIN));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Speaker", ItemType.ELECTRONICS));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    int staleVersion = auction.getVersion();
    auction.setStatus(AuctionStatus.RUNNING);
    auctionDAO.update(auction);

    assertThrows(
        ServiceException.class,
        () -> auctionService.cancelAuction(auction.getId(), admin.getId(), staleVersion));

    Auction found = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(AuctionStatus.RUNNING, found.getStatus());
  }

  @Test
  void getAllAuctions_shouldUseCacheUntilInvalidated() {
    Item firstItem = itemDAO.save(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));
    auctionService.createAuction(
        TestFixtures.auction(
            firstItem.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    var firstLoad = auctionService.getAllAuctions();

    Item secondItem = itemDAO.save(TestFixtures.item(seller.getId(), "Bike", ItemType.VEHICLE));
    auctionDAO.save(
        TestFixtures.auction(
            secondItem.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 2000L));
    var cachedLoad = auctionService.getAllAuctions();

    assertEquals(1, firstLoad.size());
    assertEquals(1, cachedLoad.size());

    auctionService.invalidateCache();
    var refreshed = auctionService.getAllAuctions();

    assertEquals(2, refreshed.size());
  }

  @Test
  void updateStatusAndTimes_shouldPersistChanges() {
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Watch", ItemType.ART));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    LocalDateTime newEndTime = LocalDateTime.now().plusHours(2);

    auctionService.updateStatus(auction.getId(), AuctionStatus.RUNNING);
    auctionService.setStartTime(auction.getId(), LocalDateTime.now());
    auctionService.setEndTime(auction.getId(), newEndTime);

    Auction found = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(AuctionStatus.RUNNING, found.getStatus());
    assertNotNull(found.getStartTime());
    assertTrue(found.getEndTime().isAfter(LocalDateTime.now().plusMinutes(30)));
  }
}
