package app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.TestFixtures;
import app.dao.AuctionDao;
import app.dao.BaseDaoTest;
import app.dao.BidDao;
import app.dao.ItemDao;
import app.dao.UserDao;
import app.dao.impl.MySqlAuctionDao;
import app.dao.impl.MySqlBidDao;
import app.dao.impl.MySqlItemDao;
import app.dao.impl.MySqlUserDao;
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

class AuctionServiceTest extends BaseDaoTest {
  private UserDao userDao;
  private ItemDao itemDao;
  private AuctionDao auctionDao;
  private BidDao bidDao;
  private AuctionService auctionService;
  private TransactionManager transactionManager;
  private User seller;

  @BeforeEach
  void setUp() {
    userDao = new MySqlUserDao();
    itemDao = new MySqlItemDao();
    auctionDao = new MySqlAuctionDao();
    bidDao = new MySqlBidDao();
    transactionManager = new TransactionManager();
    auctionService = new AuctionService(auctionDao, bidDao, itemDao, userDao, transactionManager);
    seller = userDao.save(TestFixtures.user(TestFixtures.unique("seller"), UserRole.SELLER));
  }

  @Test
  void createAuction_shouldPersistFutureAuction() {
    Item item = itemDao.save(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));

    Auction saved =
        auctionService.createAuction(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));

    Auction found = auctionDao.findById(saved.getId()).orElseThrow();
    assertEquals(AuctionStatus.OPEN, found.getStatus());
    assertEquals(1000L, found.getHighestBid());
  }

  @Test
  void createAuction_shouldRejectPastEndTime() {
    Item item = itemDao.save(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));
    Auction auction =
        TestFixtures.auction(
            item.getId(), seller.getId(), LocalDateTime.now().minusMinutes(1), 1000L);

    assertThrows(ServiceException.class, () -> auctionService.createAuction(auction));
  }

  @Test
  void createAndStartAuction_shouldPersistRunningAuction() {
    Item item = itemDao.save(TestFixtures.item(seller.getId(), "Camera", ItemType.ELECTRONICS));

    Auction saved = auctionService.createAndStartAuction(item.getId(), seller.getId(), 1000L, 10);

    Auction found = auctionDao.findById(saved.getId()).orElseThrow();
    assertEquals(AuctionStatus.RUNNING, found.getStatus());
    assertNotNull(found.getStartTime());
  }

  @Test
  void getAuctionDetail_shouldUseHighestBidWhenBidsExist() {
    User bidder = userDao.save(TestFixtures.user(TestFixtures.unique("bidder"), UserRole.BIDDER));
    Item item = itemDao.save(TestFixtures.item(seller.getId(), "Laptop", ItemType.ELECTRONICS));
    Auction auction =
        auctionDao.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    bidDao.insertBid(auction.getId(), bidder.getId(), 1500L, false);

    var detail = auctionService.getAuctionDetail(auction.getId());

    assertEquals(auction.getId(), detail.auctionId());
    assertEquals("Laptop", detail.itemName());
    assertEquals(1500L, detail.currentPrice());
    assertEquals(auction.getVersion(), detail.version());
  }

  @Test
  void handleCompletion_shouldFinishExpiredAuctionAndSetWinner() {
    User bidder = userDao.save(TestFixtures.user(TestFixtures.unique("bidder"), UserRole.BIDDER));
    Item item = itemDao.save(TestFixtures.item(seller.getId(), "Tablet", ItemType.ELECTRONICS));
    Auction auction =
        TestFixtures.auction(
            item.getId(), seller.getId(), LocalDateTime.now().minusMinutes(1), 1000L);
    auction.start();
    auction = auctionDao.save(auction);
    bidDao.insertBid(auction.getId(), bidder.getId(), 1500L, false);

    auctionService.handleCompletion(auction.getId());

    Auction finished = auctionDao.findById(auction.getId()).orElseThrow();
    assertEquals(AuctionStatus.FINISHED, finished.getStatus());
    assertEquals(bidder.getId(), finished.getWinnerId());
  }

  @Test
  void cancelAuctionByAdmin_shouldRejectStaleVersion() {
    User admin = userDao.save(TestFixtures.user(TestFixtures.unique("admin"), UserRole.ADMIN));
    Item item = itemDao.save(TestFixtures.item(seller.getId(), "Speaker", ItemType.ELECTRONICS));
    Auction auction =
        auctionDao.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    int staleVersion = auction.getVersion();
    auction.setStatus(AuctionStatus.RUNNING);
    auctionDao.update(auction);

    assertThrows(
        ServiceException.class,
        () -> auctionService.cancelAuctionByAdmin(auction.getId(), admin.getId(), staleVersion));

    Auction found = auctionDao.findById(auction.getId()).orElseThrow();
    assertEquals(AuctionStatus.RUNNING, found.getStatus());
  }

  @Test
  void getAuctionSummaries_shouldUseCacheUntilInvalidated() {
    Item firstItem = itemDao.save(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));
    auctionService.createAuction(
        TestFixtures.auction(
            firstItem.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    var firstLoad = auctionService.getAuctionSummaries();

    Item secondItem = itemDao.save(TestFixtures.item(seller.getId(), "Bike", ItemType.VEHICLE));
    auctionDao.save(
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
    Item item = itemDao.save(TestFixtures.item(seller.getId(), "Watch", ItemType.ART));
    Auction auction =
        auctionDao.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    LocalDateTime newEndTime = LocalDateTime.now().plusHours(2);

    auctionService.updateStatus(auction.getId(), AuctionStatus.RUNNING);
    auctionService.setStartTime(auction.getId(), LocalDateTime.now());
    auctionService.setEndTime(auction.getId(), newEndTime);

    Auction found = auctionDao.findById(auction.getId()).orElseThrow();
    assertEquals(AuctionStatus.RUNNING, found.getStatus());
    assertNotNull(found.getStartTime());
    assertTrue(found.getEndTime().isAfter(LocalDateTime.now().plusMinutes(30)));
  }
}
