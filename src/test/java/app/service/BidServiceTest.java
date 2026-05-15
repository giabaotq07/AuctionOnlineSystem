package app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class BidServiceTest extends BaseDaoTest {
  private UserDao userDao;
  private ItemDao itemDao;
  private AuctionDao auctionDao;
  private BidDao bidDao;
  private BidService bidService;
  private User seller;
  private User bidder;
  private Item item;

  @BeforeEach
  void setUp() {
    userDao = new MySqlUserDao();
    itemDao = new MySqlItemDao();
    auctionDao = new MySqlAuctionDao();
    bidDao = new MySqlBidDao();
    bidService =
        new BidService(
            bidDao,
            auctionDao,
            userDao,
            new TransactionManager(),
            new BidValidator(),
            new AntiSnipeService());
    seller = userDao.save(TestFixtures.user(TestFixtures.unique("seller"), UserRole.SELLER));
    bidder = userDao.save(TestFixtures.user(TestFixtures.unique("bidder"), UserRole.BIDDER));
    item = itemDao.save(TestFixtures.item(seller.getId(), "Laptop", ItemType.ELECTRONICS));
  }

  @Test
  void placeBid_shouldInsertBidAndUpdateAuction() {
    Auction auction = runningAuction(1000L, LocalDateTime.now().plusMinutes(10));

    bidService.placeBid(auction.getId(), bidder.getId(), 1200L);

    Auction updated = auctionDao.findById(auction.getId()).orElseThrow();
    var highest = bidDao.findHighestBid(auction.getId()).orElseThrow();
    assertEquals(1200L, updated.getHighestBid());
    assertEquals(bidder.getId(), updated.getWinnerId());
    assertEquals(1200L, highest.getAmount());
    assertEquals(bidder.getId(), highest.getBidderId());
  }

  @Test
  void placeBid_shouldRejectBidBelowMinimumAndRollback() {
    Auction auction = runningAuction(1000L, LocalDateTime.now().plusMinutes(10));

    assertThrows(
        ServiceException.class, () -> bidService.placeBid(auction.getId(), bidder.getId(), 1000L));

    Auction unchanged = auctionDao.findById(auction.getId()).orElseThrow();
    assertEquals(1000L, unchanged.getHighestBid());
    assertTrue(bidDao.findHighestBid(auction.getId()).isEmpty());
  }

  @Test
  void placeBid_shouldRejectAuctionThatIsNotRunning() {
    Auction auction =
        auctionDao.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusMinutes(10), 1000L));

    assertThrows(
        ServiceException.class, () -> bidService.placeBid(auction.getId(), bidder.getId(), 1200L));
  }

  @Test
  void placeBid_shouldExtendAuctionWhenBidIsNearEndTime() {
    LocalDateTime originalEndTime = LocalDateTime.now().plusSeconds(10);
    Auction auction = runningAuction(1000L, originalEndTime);

    bidService.placeBid(auction.getId(), bidder.getId(), 1200L);

    Auction updated = auctionDao.findById(auction.getId()).orElseThrow();
    assertEquals(1, updated.getExtendedCount());
    assertTrue(updated.getEndTime().isAfter(originalEndTime.plusSeconds(50)));
  }

  @Test
  void placeBid_shouldReturnCurrentHighestBid() {
    Auction auction = runningAuction(1000L, LocalDateTime.now().plusMinutes(10));

    var response = bidService.placeBid(auction.getId(), bidder.getId(), 1300L);

    assertTrue(response.success());
    assertEquals(auction.getId(), response.auctionId());
    assertEquals(1300L, response.highestBidAmount());
    assertEquals(bidder.getId(), response.bidderId());
  }

  @Test
  void bidDaoQueries_shouldReturnPersistedBids() {
    Auction auction = runningAuction(1000L, LocalDateTime.now().plusMinutes(10));
    User secondBidder =
        userDao.save(TestFixtures.user(TestFixtures.unique("second_bidder"), UserRole.BIDDER));
    bidService.placeBid(auction.getId(), bidder.getId(), 1200L);
    bidService.placeBid(auction.getId(), secondBidder.getId(), 1400L);

    assertEquals(2, bidDao.findByAuction(auction.getId()).size());
    assertEquals(2, bidDao.findByAuctionOrderByTime(auction.getId()).size());
    assertEquals(1400L, bidDao.findHighestBid(auction.getId()).orElseThrow().getAmount());
  }

  private Auction runningAuction(long currentPrice, LocalDateTime endTime) {
    Auction auction = TestFixtures.auction(item.getId(), seller.getId(), endTime, currentPrice);
    auction.start();
    auction = auctionDao.save(auction);
    auctionDao.update(auction);
    assertEquals(
        AuctionStatus.RUNNING, auctionDao.findById(auction.getId()).orElseThrow().getStatus());
    return auction;
  }
}
