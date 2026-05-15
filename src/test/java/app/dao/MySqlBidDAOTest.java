package app.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.TestFixtures;
import app.dao.impl.MySqlAuctionDao;
import app.dao.impl.MySqlBidDao;
import app.dao.impl.MySqlItemDao;
import app.dao.impl.MySqlUserDao;
import app.enums.ItemType;
import app.enums.UserRole;
import app.models.Auction;
import app.models.Item;
import app.models.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MySqlBidDaoTest extends BaseDaoTest {
  private UserDao userDao;
  private ItemDao itemDao;
  private AuctionDao auctionDao;
  private BidDao bidDao;
  private User seller;
  private User bidder;
  private Auction auction;

  @BeforeEach
  void setUp() {
    userDao = new MySqlUserDao();
    itemDao = new MySqlItemDao();
    auctionDao = new MySqlAuctionDao();
    bidDao = new MySqlBidDao();

    seller = userDao.save(TestFixtures.user(TestFixtures.unique("seller"), UserRole.SELLER));
    bidder = userDao.save(TestFixtures.user(TestFixtures.unique("bidder"), UserRole.BIDDER));
    Item item = itemDao.save(TestFixtures.item(seller.getId(), "Laptop", ItemType.ELECTRONICS));
    auction =
        auctionDao.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    auction.start();
    auctionDao.update(auction);
  }

  @Test
  void insertBid_shouldPersistBidAndExposeHighestBid() {
    User secondBidder =
        userDao.save(TestFixtures.user(TestFixtures.unique("second_bidder"), UserRole.BIDDER));

    bidDao.insertBid(auction.getId(), bidder.getId(), 1200L, false);
    bidDao.insertBid(auction.getId(), secondBidder.getId(), 1500L, true);

    var highest = bidDao.findHighestBid(auction.getId()).orElseThrow();
    assertEquals(secondBidder.getId(), highest.getBidderId());
    assertEquals(1500L, highest.getAmount());
    assertTrue(highest.isAutoBid());
  }

  @Test
  void findByAuction_shouldReturnBidsOrderedByAmountDescending() {
    User secondBidder =
        userDao.save(TestFixtures.user(TestFixtures.unique("second_bidder"), UserRole.BIDDER));
    User thirdBidder =
        userDao.save(TestFixtures.user(TestFixtures.unique("third_bidder"), UserRole.BIDDER));
    bidDao.insertBid(auction.getId(), bidder.getId(), 1200L, false);
    bidDao.insertBid(auction.getId(), secondBidder.getId(), 1500L, false);
    bidDao.insertBid(auction.getId(), thirdBidder.getId(), 1300L, false);

    var bids = bidDao.findByAuction(auction.getId());

    assertEquals(3, bids.size());
    assertEquals(1500L, bids.get(0).getAmount());
    assertEquals(1300L, bids.get(1).getAmount());
    assertEquals(1200L, bids.get(2).getAmount());
  }

  @Test
  void existsByAuctionAndUser_shouldDetectBidPresence() {
    bidDao.insertBid(auction.getId(), bidder.getId(), 1200L, false);

    assertTrue(bidDao.existsByAuctionAndUser(auction.getId(), bidder.getId()));
    assertFalse(bidDao.existsByAuctionAndUser(auction.getId(), seller.getId()));
  }

  @Test
  void findHighestBid_shouldReturnEmptyWhenSessionHasNoBids() {
    assertTrue(bidDao.findHighestBid(auction.getId()).isEmpty());
    assertTrue(bidDao.findByAuctionOrderByTime(auction.getId()).isEmpty());
  }
}
