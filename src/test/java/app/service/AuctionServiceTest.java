package app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.TestFixtures;
import app.DAO.AuctionDAO;
import app.DAO.BaseDAOTest;
import app.DAO.BidDAO;
import app.DAO.ItemDAO;
import app.DAO.UserDAO;
import app.DAO.impl.MySqlAuctionDAO;
import app.DAO.impl.MySqlBidDAO;
import app.DAO.impl.MySqlItemDAO;
import app.DAO.impl.MySqlUserDAO;
import app.database.TransactionManager;
import app.enums.AuctionStatus;
import app.enums.ItemType;
import app.enums.UserRole;
import app.exception.ServiceException;
import app.models.Auction;
import app.models.Item;
import app.models.User;
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
  void getAuctionDetail_shouldUseHighestBidWhenBidsExist() {
    User bidder = userDAO.save(TestFixtures.user(TestFixtures.unique("bidder"), UserRole.BIDDER));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Laptop", ItemType.ELECTRONICS));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    bidDAO.insertBid(auction.getId(), bidder.getId(), 1500L, false);

    var detail = auctionService.getAuctionDetail(auction.getId());

    assertEquals(auction.getId(), detail.auctionId());
    assertEquals("Laptop", detail.itemName());
    assertEquals(1500L, detail.currentPrice());
    assertEquals(auction.getVersion(), detail.version());
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
  void cancelAuctionByAdmin_shouldRejectStaleVersion() {
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
        () -> auctionService.cancelAuctionByAdmin(auction.getId(), admin.getId(), staleVersion));

    Auction found = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(AuctionStatus.RUNNING, found.getStatus());
  }

  @Test
  void getAuctionSummaries_shouldUseCacheUntilInvalidated() {
    Item firstItem = itemDAO.save(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));
    auctionService.createAuction(
        TestFixtures.auction(
            firstItem.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    var firstLoad = auctionService.getAuctionSummaries();

    Item secondItem = itemDAO.save(TestFixtures.item(seller.getId(), "Bike", ItemType.VEHICLE));
    auctionDAO.save(
        TestFixtures.auction(
            secondItem.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 2000L));
    var cachedLoad = auctionService.getAuctionSummaries();

    assertEquals(1, firstLoad.size());
    assertEquals(1, cachedLoad.size());

    auctionService.invalidateCache();
    var refreshed = auctionService.getAuctionSummaries();

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
