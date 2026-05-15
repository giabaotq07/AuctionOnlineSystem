package app.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.TestFixtures;
import app.dao.impl.MySqlAuctionDao;
import app.dao.impl.MySqlItemDao;
import app.dao.impl.MySqlUserDao;
import app.database.DatabaseConnection;
import app.enums.AuctionStatus;
import app.enums.ItemType;
import app.enums.UserRole;
import app.models.Auction;
import app.models.Item;
import app.models.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MySqlAuctionDaoTest extends BaseDaoTest {
  private UserDao userDao;
  private ItemDao itemDao;
  private AuctionDao auctionDao;
  private User seller;

  @BeforeEach
  void setUp() {
    userDao = new MySqlUserDao();
    itemDao = new MySqlItemDao();
    auctionDao = new MySqlAuctionDao();
    seller = userDao.save(TestFixtures.user(TestFixtures.unique("seller"), UserRole.SELLER));
  }

  @Test
  void save_shouldPersistAuctionAndReturnGeneratedId() {
    Item item = itemDao.save(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));
    Auction saved =
        auctionDao.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));

    assertTrue(saved.getId() > 0);
    Auction found = auctionDao.findById(saved.getId()).orElseThrow();
    assertEquals(item.getId(), found.getItemId());
    assertEquals(seller.getId(), found.getSellerId());
    assertEquals(AuctionStatus.OPEN, found.getStatus());
    assertEquals(1000L, found.getHighestBid());
    assertEquals(0, found.getVersion());
  }

  @Test
  void update_shouldPersistStatusWinnerAndBidState() {
    User bidder = userDao.save(TestFixtures.user(TestFixtures.unique("bidder"), UserRole.BIDDER));
    Item item = itemDao.save(TestFixtures.item(seller.getId(), "Camera", ItemType.ELECTRONICS));
    Auction auction =
        auctionDao.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    auction.start();
    auction.updateHighestBid(1500L, bidder.getId());
    auction.setStatus(AuctionStatus.FINISHED);
    auction.setEndTime(LocalDateTime.now().plusMinutes(10));

    boolean updated = auctionDao.update(auction);

    assertTrue(updated);
    Auction found = auctionDao.findById(auction.getId()).orElseThrow();
    assertEquals(AuctionStatus.FINISHED, found.getStatus());
    assertEquals(bidder.getId(), found.getWinnerId());
    assertEquals(1500L, found.getHighestBid());
    assertTrue(found.getStartTime() != null);
    assertEquals(auction.getVersion(), found.getVersion());
    assertEquals(1, found.getVersion());
  }

  @Test
  void updateIfVersionMatches_shouldUpdateOnlyWhenExpectedVersionMatches() throws Exception {
    Item item = itemDao.save(TestFixtures.item(seller.getId(), "Watch", ItemType.ART));
    Auction auction =
        auctionDao.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    int initialVersion = auction.getVersion();

    auction.setStatus(AuctionStatus.CANCELED);
    boolean updated;
    try (var conn = DatabaseConnection.getDataSource().getConnection()) {
      updated = auctionDao.updateIfVersionMatches(conn, auction, initialVersion);
    }

    Auction found = auctionDao.findById(auction.getId()).orElseThrow();
    assertTrue(updated);
    assertEquals(AuctionStatus.CANCELED, found.getStatus());
    assertEquals(initialVersion + 1, found.getVersion());

    found.setStatus(AuctionStatus.RUNNING);
    boolean staleUpdate;
    try (var conn = DatabaseConnection.getDataSource().getConnection()) {
      staleUpdate = auctionDao.updateIfVersionMatches(conn, found, initialVersion);
    }

    Auction unchanged = auctionDao.findById(auction.getId()).orElseThrow();
    assertFalse(staleUpdate);
    assertEquals(AuctionStatus.CANCELED, unchanged.getStatus());
    assertEquals(initialVersion + 1, unchanged.getVersion());
  }

  @Test
  void findByStatusAndSeller_shouldFilterAuctions() {
    Item firstItem = itemDao.save(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));
    Item secondItem = itemDao.save(TestFixtures.item(seller.getId(), "Bike", ItemType.VEHICLE));
    Auction openAuction =
        auctionDao.save(
            TestFixtures.auction(
                firstItem.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));
    Auction runningAuction =
        auctionDao.save(
            TestFixtures.auction(
                secondItem.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 2000L));
    runningAuction.start();
    auctionDao.update(runningAuction);

    var openAuctions = auctionDao.findByStatus(AuctionStatus.OPEN);
    var sellerAuctions = auctionDao.findBySeller(seller.getId());

    assertEquals(1, openAuctions.size());
    assertEquals(openAuction.getId(), openAuctions.getFirst().getId());
    assertEquals(2, sellerAuctions.size());
  }

  @Test
  void delete_shouldRemoveAuction() {
    Item item = itemDao.save(TestFixtures.item(seller.getId(), "Tablet", ItemType.ELECTRONICS));
    Auction auction =
        auctionDao.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusHours(1), 1000L));

    assertTrue(auctionDao.delete(auction.getId()));

    assertFalse(auctionDao.findById(auction.getId()).isPresent());
  }
}
