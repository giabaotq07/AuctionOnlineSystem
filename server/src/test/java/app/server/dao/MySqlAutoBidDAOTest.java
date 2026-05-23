package app.server.dao;

import static org.junit.jupiter.api.Assertions.*;

import app.TestFixtures;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
import app.common.models.Auction;
import app.common.models.AutoBid;
import app.common.models.Item;
import app.common.models.User;
import app.server.dao.impl.*;
import app.server.database.DatabaseConnection;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Lop kiem thu cho MySqlAutoBidDAO. Ke thua tu BaseDAOTest de chay tich hop tren H2 Database. Viet
 * bang tieng Viet khong dau theo dung quy dinh.
 */
public class MySqlAutoBidDAOTest extends BaseDAOTest {
  private UserDAO userDAO;
  private ItemDAO itemDAO;
  private AuctionDAO auctionDAO;
  private AutoBidDAO autoBidDAO;

  private User seller;
  private User bidder;
  private Item item;
  private Auction auction;

  @BeforeEach
  void setUp() {
    userDAO = new MySqlUserDAO();
    itemDAO = new MySqlItemDAO();
    auctionDAO = new MySqlAuctionDAO();
    autoBidDAO = new MySqlAutoBidDAO();

    // Khoi tao du lieu mau
    seller = userDAO.save(TestFixtures.user(TestFixtures.unique("seller"), UserRole.SELLER));
    bidder = userDAO.save(TestFixtures.user(TestFixtures.unique("bidder"), UserRole.BIDDER));
    item = itemDAO.save(TestFixtures.item(seller.getId(), "Antique Vase", ItemType.ART));
    auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusDays(1), 1000L));
  }

  @Test
  void testSaveAndFindAutoBid() {
    AutoBid autoBid =
        new AutoBid(
            0,
            auction.getId(),
            bidder.getId(),
            5000L,
            100L,
            true,
            LocalDateTime.now(),
            LocalDateTime.now());
    AutoBid saved = autoBidDAO.save(autoBid);

    assertNotNull(saved);
    assertTrue(saved.getId() > 0);
    assertEquals(auction.getId(), saved.getAuctionId());
    assertEquals(bidder.getId(), saved.getUserId());
    assertEquals(5000L, saved.getMaxAmount());

    // Tim kiem bang FindById
    AutoBid found = autoBidDAO.findById(saved.getId()).orElse(null);
    assertNotNull(found);
    assertEquals(saved.getId(), found.getId());

    // Tim kiem bang FindByAuctionAndUser
    AutoBid foundByUser =
        autoBidDAO.findByAuctionAndUser(auction.getId(), bidder.getId()).orElse(null);
    assertNotNull(foundByUser);
    assertEquals(saved.getId(), foundByUser.getId());
  }

  @Test
  void testFindByAuctionAndEnabled() {
    AutoBid autoBid1 =
        new AutoBid(
            0,
            auction.getId(),
            bidder.getId(),
            5000L,
            100L,
            true,
            LocalDateTime.now(),
            LocalDateTime.now());
    autoBidDAO.save(autoBid1);

    User bidder2 = userDAO.save(TestFixtures.user(TestFixtures.unique("bidder2"), UserRole.BIDDER));
    AutoBid autoBid2 =
        new AutoBid(
            0,
            auction.getId(),
            bidder2.getId(),
            6000L,
            200L,
            false,
            LocalDateTime.now(),
            LocalDateTime.now());
    autoBidDAO.save(autoBid2);

    // Tim kiem tat ca auto bid cua phien dau gia
    List<AutoBid> allBids = autoBidDAO.findByAuction(auction.getId());
    assertEquals(2, allBids.size());

    // Tim kiem cac auto bid dang hoat dong (enabled = true)
    List<AutoBid> enabledBids = autoBidDAO.findEnabledByAuction(auction.getId());
    assertEquals(1, enabledBids.size());
    assertEquals(bidder.getId(), enabledBids.get(0).getUserId());
  }

  @Test
  void testUpdateAutoBid() {
    AutoBid autoBid =
        new AutoBid(
            0,
            auction.getId(),
            bidder.getId(),
            5000L,
            100L,
            true,
            LocalDateTime.now(),
            LocalDateTime.now());
    AutoBid saved = autoBidDAO.save(autoBid);

    saved.setMaxAmount(7000L);
    saved.setIncrementAmount(300L);
    saved.setEnabled(false);

    boolean updated = autoBidDAO.update(saved);
    assertTrue(updated);

    AutoBid found = autoBidDAO.findById(saved.getId()).orElse(null);
    assertNotNull(found);
    assertEquals(7000L, found.getMaxAmount());
    assertEquals(300L, found.getIncrementAmount());
    assertFalse(found.isEnabled());
  }

  @Test
  void testDeleteAndSetEnabled() {
    AutoBid autoBid =
        new AutoBid(
            0,
            auction.getId(),
            bidder.getId(),
            5000L,
            100L,
            true,
            LocalDateTime.now(),
            LocalDateTime.now());
    AutoBid saved = autoBidDAO.save(autoBid);

    // Cap nhat enabled bang setEnabled
    boolean statusChanged = autoBidDAO.setEnabled(saved.getId(), false);
    assertTrue(statusChanged);

    AutoBid found = autoBidDAO.findById(saved.getId()).orElse(null);
    assertNotNull(found);
    assertFalse(found.isEnabled());

    // Xoa auto bid
    boolean deleted = autoBidDAO.delete(saved.getId());
    assertTrue(deleted);

    assertFalse(autoBidDAO.findById(saved.getId()).isPresent());
  }

  @Test
  void testSaveAndFindWithConnection() throws Exception {
    AutoBid autoBid =
        new AutoBid(
            0,
            auction.getId(),
            bidder.getId(),
            5000L,
            100L,
            true,
            LocalDateTime.now(),
            LocalDateTime.now());

    try (var conn = DatabaseConnection.getDataSource().getConnection()) {
      AutoBid saved = autoBidDAO.save(conn, autoBid);
      assertNotNull(saved);
      assertTrue(saved.getId() > 0);

      AutoBid found =
          autoBidDAO.findByAuctionAndUser(conn, auction.getId(), bidder.getId()).orElse(null);
      assertNotNull(found);
      assertEquals(saved.getId(), found.getId());

      List<AutoBid> enabled = autoBidDAO.findEnabledByAuction(conn, auction.getId());
      assertEquals(1, enabled.size());

      saved.setMaxAmount(8000L);
      boolean updated = autoBidDAO.update(conn, saved);
      assertTrue(updated);
    }
  }
}
