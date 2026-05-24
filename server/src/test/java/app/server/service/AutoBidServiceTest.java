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
import app.server.dao.AutoBidDAO;
import app.server.dao.BidDAO;
import app.server.dao.ItemDAO;
import app.server.dao.UserDAO;
import app.server.dao.impl.MySqlAuctionDAO;
import app.server.dao.impl.MySqlAutoBidDAO;
import app.server.dao.impl.MySqlBidDAO;
import app.server.dao.impl.MySqlItemDAO;
import app.server.dao.impl.MySqlUserDAO;
import app.server.database.TransactionManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** AutoBidServiceTest. */
public class AutoBidServiceTest extends app.server.dao.BaseDAOTest {
  private UserDAO userDAO;
  private ItemDAO itemDAO;
  private AuctionDAO auctionDAO;
  private BidDAO bidDAO;
  private AutoBidDAO autoBidDAO;
  private TransactionManager transactionManager;
  private AutoBidService autoBidService;
  private BidService bidService;
  private AuctionSettlementService settlementService;
  private User seller;
  private User bidder1;
  private User bidder2;
  private Auction auction;

  @BeforeEach
  void setUp() {
    userDAO = new MySqlUserDAO();
    itemDAO = new MySqlItemDAO();
    auctionDAO = new MySqlAuctionDAO();
    bidDAO = new MySqlBidDAO();
    autoBidDAO = new MySqlAutoBidDAO();
    transactionManager = new TransactionManager();
    BidValidator bidValidator = new BidValidator();
    AntiSnipeService antiSnipeService = new AntiSnipeService();
    autoBidService =
        new AutoBidService(
            autoBidDAO, auctionDAO, bidDAO, itemDAO, userDAO, transactionManager, bidValidator);
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
    settlementService = new AuctionSettlementService(bidDAO, userDAO, autoBidDAO);

    seller =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("seller_auto"), UserRole.SELLER, BigDecimal.valueOf(1000)));
    bidder1 =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidder_auto_1"), UserRole.BIDDER, BigDecimal.valueOf(5000)));
    bidder2 =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidder_auto_2"), UserRole.BIDDER, BigDecimal.valueOf(5000)));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Auto Bid Item", ItemType.ART));
    auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusDays(1), 1000L));
    auction.start();
    auctionDAO.update(auction);
  }

  @Test
  void setAutoBidCreatesRecordAndFreezesMaxAmountWithoutBiddingImmediately() {
    autoBidService.setAutoBid(auction.getId(), bidder1, 2000L, 100L);

    assertTrue(autoBidDAO.findByAuctionAndUser(auction.getId(), bidder1.getId()).isPresent());
    Auction stored = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(1000L, stored.getHighestBid());
    assertNull(stored.getWinnerId());
    assertTrue(bidDAO.findByAuction(auction.getId()).isEmpty());

    User storedBidder = userDAO.findById(bidder1.getId()).orElseThrow();
    assertEquals(
        BigDecimal.valueOf(2000).stripTrailingZeros(),
        storedBidder
            .getWallet()
            .getFrozenAmount(String.valueOf(auction.getId()))
            .stripTrailingZeros());
  }

  @Test
  void twoAutoBidsResolveAfterManualBid() {
    autoBidService.setAutoBid(auction.getId(), bidder1, 3000L, 100L);
    autoBidService.setAutoBid(auction.getId(), bidder2, 2500L, 100L);

    Auction pending = auctionDAO.findById(auction.getId()).orElseThrow();
    assertNull(pending.getWinnerId());
    assertEquals(1000L, pending.getHighestBid());

    Auction stored = bidService.placeBid(auction.getId(), bidder2, 1100L);
    assertEquals(bidder1.getId(), stored.getWinnerId());
    assertEquals(2600L, stored.getHighestBid());
  }

  @Test
  void equalMaxBidKeepsEarlierAutoBidAsWinner() {
    autoBidService.setAutoBid(auction.getId(), bidder1, 2000L, 100L);
    autoBidService.setAutoBid(auction.getId(), bidder2, 2000L, 100L);

    Auction stored = bidService.placeBid(auction.getId(), bidder2, 1100L);

    assertEquals(bidder1.getId(), stored.getWinnerId());
    assertEquals(2000L, stored.getHighestBid());
  }

  @Test
  void overwritingAutoBidResetsTiePriority() {
    autoBidService.setAutoBid(auction.getId(), bidder1, 1500L, 100L);
    autoBidService.setAutoBid(auction.getId(), bidder2, 2000L, 100L);
    autoBidService.setAutoBid(auction.getId(), bidder1, 2000L, 100L);

    Auction stored = bidService.placeBid(auction.getId(), bidder1, 1100L);

    assertEquals(bidder2.getId(), stored.getWinnerId());
    assertEquals(2000L, stored.getHighestBid());
  }

  @Test
  void firstAutoBidDoesNotImmediatelyOutbidManualLeader() {
    bidService.placeBid(auction.getId(), bidder2, 1100L);

    autoBidService.setAutoBid(auction.getId(), bidder1, 2000L, 100L);

    Auction stored = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(bidder2.getId(), stored.getWinnerId());
    assertEquals(1100L, stored.getHighestBid());
    assertEquals(1, bidDAO.findByAuction(auction.getId()).size());
  }

  @Test
  void lowerCompetingAutoBidIsDisabledAfterManualBidResolution() {
    autoBidService.setAutoBid(auction.getId(), bidder1, 3000L, 100L);
    autoBidService.setAutoBid(auction.getId(), bidder2, 2000L, 100L);

    bidService.placeBid(auction.getId(), bidder2, 1100L);

    Auction stored = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(bidder1.getId(), stored.getWinnerId());
    assertEquals(2100L, stored.getHighestBid());
    assertFalse(
        autoBidDAO
            .findByAuctionAndUser(auction.getId(), bidder2.getId())
            .orElseThrow()
            .isEnabled());

    User storedBidder = userDAO.findById(bidder2.getId()).orElseThrow();
    assertEquals(
        BigDecimal.valueOf(1100).stripTrailingZeros(),
        storedBidder
            .getWallet()
            .getFrozenAmount(String.valueOf(auction.getId()))
            .stripTrailingZeros());
  }

  @Test
  void currentLeaderCanOverwriteAutoBidDownToCurrentPrice() {
    autoBidService.setAutoBid(auction.getId(), bidder1, 3000L, 100L);

    Auction ledAuction = bidService.placeBid(auction.getId(), bidder2, 1100L);
    assertEquals(bidder1.getId(), ledAuction.getWinnerId());
    assertEquals(1200L, ledAuction.getHighestBid());

    autoBidService.setAutoBid(auction.getId(), bidder1, 1200L, 100L);

    Auction stored = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(bidder1.getId(), stored.getWinnerId());
    assertEquals(1200L, stored.getHighestBid());
    assertEquals(2, bidDAO.findByAuction(auction.getId()).size());

    User storedBidder = userDAO.findById(bidder1.getId()).orElseThrow();
    assertEquals(
        BigDecimal.valueOf(1200).stripTrailingZeros(),
        storedBidder
            .getWallet()
            .getFrozenAmount(String.valueOf(auction.getId()))
            .stripTrailingZeros());
  }

  @Test
  void currentLeaderCannotOverwriteAutoBidBelowCurrentPrice() {
    autoBidService.setAutoBid(auction.getId(), bidder1, 3000L, 100L);
    Auction ledAuction = bidService.placeBid(auction.getId(), bidder2, 1100L);

    assertEquals(bidder1.getId(), ledAuction.getWinnerId());
    assertEquals(1200L, ledAuction.getHighestBid());
    assertThrows(
        ServiceException.class,
        () -> autoBidService.setAutoBid(auction.getId(), bidder1, 1199L, 100L));
  }

  @Test
  void disableAutoBidStopsFutureAutomaticBids() {
    autoBidService.setAutoBid(auction.getId(), bidder1, 3000L, 100L);
    autoBidService.disableAutoBid(auction.getId(), bidder1);

    User afterDisable = userDAO.findById(bidder1.getId()).orElseThrow();
    assertEquals(
        BigDecimal.ZERO.stripTrailingZeros(),
        afterDisable
            .getWallet()
            .getFrozenAmount(String.valueOf(auction.getId()))
            .stripTrailingZeros());

    Auction updated = bidService.placeBid(auction.getId(), bidder2, 1200L);

    assertEquals(bidder2.getId(), updated.getWinnerId());
    assertEquals(1200L, updated.getHighestBid());
  }

  @Test
  void settlementChargesHighestBidAndReleasesAutoBidExcess() {
    autoBidService.setAutoBid(auction.getId(), bidder1, 3000L, 100L);
    autoBidService.setAutoBid(auction.getId(), bidder2, 2000L, 100L);
    bidService.placeBid(auction.getId(), bidder2, 1100L);
    Auction stored = auctionDAO.findById(auction.getId()).orElseThrow();
    stored.setStatus(AuctionStatus.FINISHED);
    auctionDAO.update(stored);

    transactionManager.runWithoutResult(conn -> settlementService.settleWallets(conn, stored));

    User winner = userDAO.findById(bidder1.getId()).orElseThrow();
    User runnerUp = userDAO.findById(bidder2.getId()).orElseThrow();
    User paidSeller = userDAO.findById(seller.getId()).orElseThrow();
    assertEquals(
        BigDecimal.valueOf(2900).stripTrailingZeros(),
        winner.getWallet().getAvailableBalance().stripTrailingZeros());
    assertEquals(
        BigDecimal.valueOf(5000).stripTrailingZeros(),
        runnerUp.getWallet().getAvailableBalance().stripTrailingZeros());
    assertEquals(
        BigDecimal.valueOf(3100).stripTrailingZeros(),
        paidSeller.getWallet().getAvailableBalance().stripTrailingZeros());
  }

  @Test
  void setAutoBidRejectsClosedAuction() {
    auction.setStatus(AuctionStatus.FINISHED);
    auctionDAO.update(auction);

    assertThrows(
        ServiceException.class,
        () -> autoBidService.setAutoBid(auction.getId(), bidder1, 2000L, 100L));
  }

  @Test
  void setAutoBidRejectsInvalidAmounts() {
    assertThrows(
        ServiceException.class,
        () -> autoBidService.setAutoBid(auction.getId(), bidder1, 1050L, 100L));
    assertThrows(
        ServiceException.class,
        () -> autoBidService.setAutoBid(auction.getId(), bidder1, 2000L, 0L));
  }
}
