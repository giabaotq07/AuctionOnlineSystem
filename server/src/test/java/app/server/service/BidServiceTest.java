package app.server.service;

import static org.junit.jupiter.api.Assertions.*;

import app.TestFixtures;
import app.common.enums.AuctionStatus;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.models.*;
import app.server.dao.*;
import app.server.dao.BaseDAOTest;
import app.server.dao.impl.*;
import app.server.database.TransactionManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Kiem thu BidService - cac truong hop dat gia bao gom ca validation. Viet bang tieng Viet khong
 * dau theo quy dinh mentor.
 */
class BidServiceTest extends BaseDAOTest {

  private BidDAO bidDAO;
  private AuctionDAO auctionDAO;
  private ItemDAO itemDAO;
  private UserDAO userDAO;
  private AutoBidDAO autoBidDAO;
  private TransactionManager transactionManager;
  private BidService bidService;

  private User seller;
  private User bidder;
  private Item item;
  private Auction auction;

  @BeforeEach
  void setUp() {
    bidDAO = new MySqlBidDAO();
    auctionDAO = new MySqlAuctionDAO();
    itemDAO = new MySqlItemDAO();
    userDAO = new MySqlUserDAO();
    autoBidDAO = new MySqlAutoBidDAO();
    transactionManager = new TransactionManager();

    BidValidator bidValidator = new BidValidator();
    AntiSnipeService antiSnipeService = new AntiSnipeService();
    AutoBidService autoBidService =
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

    seller =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bid_seller"), UserRole.SELLER, BigDecimal.valueOf(500)));
    bidder =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bid_bidder"), UserRole.BIDDER, BigDecimal.valueOf(10000)));

    item = itemDAO.save(TestFixtures.item(seller.getId(), "Bid Item", ItemType.ART));
    Auction temp =
        TestFixtures.auction(item.getId(), seller.getId(), LocalDateTime.now().plusHours(2), 1000L);
    temp.setStartTime(LocalDateTime.now().minusMinutes(5));
    auction = auctionDAO.save(temp);
    auction.start();
    auctionDAO.update(auction);
  }

  @Test
  void placeBid_shouldSucceed_whenAuctionRunningAndAmountValid() {
    // Bidder dat gia hop le: >= highestBid + stepPrice
    Auction result = bidService.placeBid(auction.getId(), bidder, 1500L);

    assertNotNull(result);
    assertEquals(1500L, result.getHighestBid());
    assertEquals(bidder.getId(), result.getWinnerId());
  }

  @Test
  void placeBid_shouldFail_whenAuctionIdInvalid() {
    assertThrows(ServiceException.class, () -> bidService.placeBid(0, bidder, 1500L));
    assertThrows(ServiceException.class, () -> bidService.placeBid(-1, bidder, 1500L));
  }

  @Test
  void placeBid_shouldFail_whenActorNull() {
    assertThrows(ServiceException.class, () -> bidService.placeBid(auction.getId(), null, 1500L));
  }

  @Test
  void placeBid_shouldFail_whenAmountTooLow() {
    // stepPrice = 100, highestBid = 1000, minBid = 1100
    assertThrows(ServiceException.class, () -> bidService.placeBid(auction.getId(), bidder, 900L));
  }

  @Test
  void placeBid_shouldFail_whenAuctionNotRunning() {
    // Chuyen auction sang OPEN (chua chay)
    Auction openAuction = auctionDAO.findById(auction.getId()).orElseThrow();
    openAuction.setStatus(AuctionStatus.OPEN);
    auctionDAO.update(openAuction);

    assertThrows(ServiceException.class, () -> bidService.placeBid(auction.getId(), bidder, 1500L));
  }

  @Test
  void placeBid_shouldFail_whenBidderInsufficientFunds() {
    // Tao bidder co so du rat thap
    User poorBidder =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("poor_bidder"), UserRole.BIDDER, BigDecimal.valueOf(50)));

    assertThrows(
        ServiceException.class, () -> bidService.placeBid(auction.getId(), poorBidder, 1500L));
  }
}
