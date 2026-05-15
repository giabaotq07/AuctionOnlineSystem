package app.DAO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.TestFixtures;
import app.dao.AuctionDAO;
import app.dao.BidDAO;
import app.dao.ItemDAO;
import app.dao.UserDAO;
import app.dao.impl.MySqlAuctionDAO;
import app.dao.impl.MySqlBidDAO;
import app.dao.impl.MySqlItemDAO;
import app.dao.impl.MySqlUserDAO;
import app.enums.ItemType;
import app.enums.UserRole;
import app.models.Auction;
import app.models.Item;
import app.models.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MySqlBidDAOTest extends BaseDAOTest {
  private UserDAO userDAO;
  private ItemDAO itemDAO;
  private AuctionDAO auctionDAO;
  private BidDAO bidDAO;
  private User seller;
  private User bidder;
  private Auction auction;

  @BeforeEach
  void setUp() {
    userDAO = new MySqlUserDAO();
    itemDAO = new MySqlItemDAO();
    auctionDAO = new MySqlAuctionDAO();
    bidDAO = new MySqlBidDAO();

    seller = userDAO.save(TestFixtures.user(TestFixtures.unique("seller"), UserRole.SELLER));
    bidder = userDAO.save(TestFixtures.user(TestFixtures.unique("bidder"), UserRole.BIDDER));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Laptop", ItemType.ELECTRONICS));
    auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    auction.start();
    auctionDAO.update(auction);
  }

  @Test
  void insertBid_shouldPersistBidAndExposeHighestBid() {
    User secondBidder =
        userDAO.save(TestFixtures.user(TestFixtures.unique("second_bidder"), UserRole.BIDDER));

    bidDAO.insertBid(auction.getId(), bidder.getId(), 1200L, false);
    bidDAO.insertBid(auction.getId(), secondBidder.getId(), 1500L, true);

    var highest = bidDAO.findHighestBid(auction.getId()).orElseThrow();
    assertEquals(secondBidder.getId(), highest.getBidderId());
    assertEquals(1500L, highest.getAmount());
    assertTrue(highest.isAutoBid());
  }

  @Test
  void findByAuction_shouldReturnBidsOrderedByAmountDescending() {
    User secondBidder =
        userDAO.save(TestFixtures.user(TestFixtures.unique("second_bidder"), UserRole.BIDDER));
    User thirdBidder =
        userDAO.save(TestFixtures.user(TestFixtures.unique("third_bidder"), UserRole.BIDDER));
    bidDAO.insertBid(auction.getId(), bidder.getId(), 1200L, false);
    bidDAO.insertBid(auction.getId(), secondBidder.getId(), 1500L, false);
    bidDAO.insertBid(auction.getId(), thirdBidder.getId(), 1300L, false);

    var bids = bidDAO.findByAuction(auction.getId());

    assertEquals(3, bids.size());
    assertEquals(1500L, bids.get(0).getAmount());
    assertEquals(1300L, bids.get(1).getAmount());
    assertEquals(1200L, bids.get(2).getAmount());
  }

  @Test
  void existsByAuctionAndUser_shouldDetectBidPresence() {
    bidDAO.insertBid(auction.getId(), bidder.getId(), 1200L, false);

    assertTrue(bidDAO.existsByAuctionAndUser(auction.getId(), bidder.getId()));
    assertFalse(bidDAO.existsByAuctionAndUser(auction.getId(), seller.getId()));
  }

  @Test
  void findHighestBid_shouldReturnEmptyWhenSessionHasNoBids() {
    assertTrue(bidDAO.findHighestBid(auction.getId()).isEmpty());
    assertTrue(bidDAO.findByAuctionOrderByTime(auction.getId()).isEmpty());
  }
}
