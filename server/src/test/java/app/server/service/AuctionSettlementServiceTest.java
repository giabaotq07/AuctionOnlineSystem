package app.server.service;

import static org.junit.jupiter.api.Assertions.*;

import app.TestFixtures;
import app.common.enums.*;
import app.common.models.*;
import app.server.dao.*;
import app.server.dao.impl.*;
import app.server.database.DatabaseConnection;
import app.server.service.result.AuctionSettlementResult;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * AuctionSettlementServiceTest. Kiem thu tich hop logic quyet toan tien vi giua nguoi mua va nguoi
 * ban tren H2 Database.
 */
public class AuctionSettlementServiceTest extends app.server.dao.BaseDAOTest {
  private UserDAO userDAO;
  private ItemDAO itemDAO;
  private AuctionDAO auctionDAO;
  private BidDAO bidDAO;
  private AuctionSettlementService settlementService;

  private User seller;
  private User bidder1;
  private User bidder2;
  private Item item;
  private Auction auction;

  @BeforeEach
  void setUp() {
    userDAO = new MySqlUserDAO();
    itemDAO = new MySqlItemDAO();
    auctionDAO = new MySqlAuctionDAO();
    bidDAO = new MySqlBidDAO();
    settlementService = new AuctionSettlementService(bidDAO, userDAO);

    // Tao nguoi ban va nguoi dau gia
    seller =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("seller"), UserRole.SELLER, BigDecimal.valueOf(100)));
    bidder1 =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidder1"), UserRole.BIDDER, BigDecimal.valueOf(1000)));
    bidder2 =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidder2"), UserRole.BIDDER, BigDecimal.valueOf(2000)));

    item = itemDAO.save(TestFixtures.item(seller.getId(), "Buc tranh Co", ItemType.ART));
    auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusDays(1), 500L));
  }

  @Test
  public void testSettleWalletsSuccessful() throws Exception {
    try (Connection conn = DatabaseConnection.getDataSource().getConnection()) {
      // Setup bidder1 bid 600, bidder2 bid 700
      bidDAO.insertBid(conn, auction.getId(), bidder1.getId(), 600L, false);
      bidDAO.insertBid(conn, auction.getId(), bidder2.getId(), 700L, false);

      // Dong bang so tien cua bidder1 va bidder2
      bidder1.getWallet().setFrozenAmount(String.valueOf(auction.getId()), BigDecimal.valueOf(600));
      userDAO.update(conn, bidder1);

      bidder2.getWallet().setFrozenAmount(String.valueOf(auction.getId()), BigDecimal.valueOf(700));
      userDAO.update(conn, bidder2);

      // Thiet lap nguoi thang cuoc la bidder2
      auction.setWinnerId(bidder2.getId());
      auction.setStatus(AuctionStatus.FINISHED);
      auctionDAO.update(conn, auction);

      // Chay settle
      AuctionSettlementResult result = settlementService.settleWalletsWithResult(conn, auction);

      assertEquals(
          BigDecimal.valueOf(700).stripTrailingZeros(),
          result.winningAmount().stripTrailingZeros());
      assertTrue(result.settledUserIds().contains(bidder1.getId()));
      assertTrue(result.settledUserIds().contains(bidder2.getId()));
      assertTrue(result.settledUserIds().contains(seller.getId()));

      // Kiem tra so du cac ben
      User u1 = userDAO.findById(conn, bidder1.getId()).orElseThrow();
      User u2 = userDAO.findById(conn, bidder2.getId()).orElseThrow();
      User s = userDAO.findById(conn, seller.getId()).orElseThrow();

      // Bidder 1 duoc tra lai tien dong bang
      assertEquals(
          BigDecimal.valueOf(1000).stripTrailingZeros(),
          u1.getWallet().getAvailableBalance().stripTrailingZeros());
      assertEquals(
          BigDecimal.ZERO.stripTrailingZeros(),
          u1.getWallet().getFrozenAmount(String.valueOf(auction.getId())).stripTrailingZeros());

      // Bidder 2 bi tru di 700
      assertEquals(
          BigDecimal.valueOf(1300).stripTrailingZeros(),
          u2.getWallet().getAvailableBalance().stripTrailingZeros());
      assertEquals(
          BigDecimal.ZERO.stripTrailingZeros(),
          u2.getWallet().getFrozenAmount(String.valueOf(auction.getId())).stripTrailingZeros());

      // Seller nhan duoc 700, so du moi = 100 + 700 = 800
      assertEquals(
          BigDecimal.valueOf(800).stripTrailingZeros(),
          s.getWallet().getAvailableBalance().stripTrailingZeros());
    }
  }

  @Test
  public void testReleaseWallets() throws Exception {
    try (Connection conn = DatabaseConnection.getDataSource().getConnection()) {
      bidDAO.insertBid(conn, auction.getId(), bidder1.getId(), 600L, false);

      bidder1.getWallet().setFrozenAmount(String.valueOf(auction.getId()), BigDecimal.valueOf(600));
      userDAO.update(conn, bidder1);

      Set<Integer> released = settlementService.releaseWallets(conn, auction);
      assertTrue(released.contains(bidder1.getId()));

      User u1 = userDAO.findById(conn, bidder1.getId()).orElseThrow();
      assertEquals(
          BigDecimal.valueOf(1000).stripTrailingZeros(),
          u1.getWallet().getAvailableBalance().stripTrailingZeros());
      assertEquals(
          BigDecimal.ZERO.stripTrailingZeros(),
          u1.getWallet().getFrozenAmount(String.valueOf(auction.getId())).stripTrailingZeros());
    }
  }
}
