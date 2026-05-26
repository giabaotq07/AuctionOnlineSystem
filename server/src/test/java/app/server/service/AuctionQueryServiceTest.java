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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Kiem thu AuctionQueryService - cac nghiep vu tra ve du lieu dau gia tong hop (read-only). Viet
 * bang tieng Viet khong dau theo quy dinh mentor.
 */
class AuctionQueryServiceTest extends BaseDAOTest {

  private AuctionDAO auctionDAO;
  private BidDAO bidDAO;
  private ItemDAO itemDAO;
  private UserDAO userDAO;
  private TransactionManager transactionManager;
  private AuctionQueryService queryService;

  private User seller;
  private User bidder;
  private Item item;
  private Auction auction;

  @BeforeEach
  void setUp() {
    auctionDAO = new MySqlAuctionDAO();
    bidDAO = new MySqlBidDAO();
    itemDAO = new MySqlItemDAO();
    userDAO = new MySqlUserDAO();
    transactionManager = new TransactionManager();

    queryService = new AuctionQueryService(auctionDAO, bidDAO, itemDAO, userDAO);

    seller =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("seller_q"), UserRole.SELLER, BigDecimal.valueOf(1000)));
    bidder =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidder_q"), UserRole.BIDDER, BigDecimal.valueOf(5000)));

    item = itemDAO.save(TestFixtures.item(seller.getId(), "Auction Item", ItemType.ART));
    Auction temp =
        TestFixtures.auction(item.getId(), seller.getId(), LocalDateTime.now().plusHours(2), 1000L);
    temp.setStartTime(LocalDateTime.now().plusHours(1));
    auction = auctionDAO.save(temp);
  }

  @Test
  void getAuctions_shouldReturnAllAuctions() {
    List<Auction> auctions = queryService.getAuctions();
    assertFalse(auctions.isEmpty());
    // Tat ca auction phai co item va seller duoc gan
    auctions.forEach(
        a -> {
          assertNotNull(a.getItem());
          assertNotNull(a.getSeller());
        });
  }

  @Test
  void getAuctionPreviews_shouldReturnMappedPreviews() {
    var previews = queryService.getAuctionPreviews();
    assertFalse(previews.isEmpty());
  }

  @Test
  void getAuction_shouldReturnAggregateWithSellerAndItem() {
    Auction result = queryService.getAuction(auction.getId());
    assertNotNull(result);
    assertNotNull(result.getItem());
    assertNotNull(result.getSeller());
    assertEquals(auction.getId(), result.getId());
  }

  @Test
  void getAuction_shouldThrow_whenNotFound() {
    assertThrows(ServiceException.class, () -> queryService.getAuction(-999));
  }

  @Test
  void getAuctionDetail_shouldIncludeBids() {
    // Chen bid truoc
    transactionManager.runWithoutResult(
        conn -> bidDAO.insertBid(conn, auction.getId(), bidder.getId(), 1500L, false));

    // Start auction de bid hop le
    auction.start();
    auctionDAO.update(auction);

    Auction detail = queryService.getAuctionDetail(auction.getId());
    assertNotNull(detail);
    assertNotNull(detail.getBids());
    // Bid list phai duoc load
    assertFalse(detail.getBids().isEmpty());
  }

  @Test
  void isAuctionVersionCurrent_shouldReturnTrue_whenVersionMatches() {
    boolean current = queryService.isAuctionVersionCurrent(auction.getId(), auction.getVersion());
    assertTrue(current);
  }

  @Test
  void isAuctionVersionCurrent_shouldReturnFalse_whenVersionNegative() {
    boolean current = queryService.isAuctionVersionCurrent(auction.getId(), -1);
    assertFalse(current);
  }

  @Test
  void getHistoryAuctions_shouldReturnFinishedAndCanceled() {
    // Tao phien FINISHED
    Item item2 = itemDAO.save(TestFixtures.item(seller.getId(), "Finished Item", ItemType.ART));
    Auction finished =
        TestFixtures.auction(
            item2.getId(), seller.getId(), LocalDateTime.now().plusHours(2), 2000L);
    finished.setStartTime(LocalDateTime.now().plusHours(1));
    finished = auctionDAO.save(finished);
    finished.setStatus(AuctionStatus.FINISHED);
    auctionDAO.update(finished);

    // Tao phien CANCELED
    Item item3 = itemDAO.save(TestFixtures.item(seller.getId(), "Canceled Item", ItemType.ART));
    Auction canceled =
        TestFixtures.auction(
            item3.getId(), seller.getId(), LocalDateTime.now().plusHours(2), 3000L);
    canceled.setStartTime(LocalDateTime.now().plusHours(1));
    canceled = auctionDAO.save(canceled);
    canceled.setStatus(AuctionStatus.CANCELED);
    auctionDAO.update(canceled);

    // Lich su theo seller
    List<Auction> history = queryService.getHistoryAuctions(seller.getId());
    assertFalse(history.isEmpty());
    history.forEach(
        a ->
            assertTrue(
                a.getStatus() == AuctionStatus.FINISHED
                    || a.getStatus() == AuctionStatus.PAID
                    || a.getStatus() == AuctionStatus.CANCELED));
  }

  @Test
  void getHistoryAuctionPreviews_shouldReturnMapped() {
    Item item2 = itemDAO.save(TestFixtures.item(seller.getId(), "Paid Item", ItemType.ART));
    Auction paid =
        TestFixtures.auction(
            item2.getId(), seller.getId(), LocalDateTime.now().plusHours(2), 1000L);
    paid.setStartTime(LocalDateTime.now().plusHours(1));
    paid = auctionDAO.save(paid);
    paid.setStatus(AuctionStatus.PAID);
    auctionDAO.update(paid);

    var previews = queryService.getHistoryAuctionPreviews(seller.getId());
    assertFalse(previews.isEmpty());
  }

  @Test
  void getAuctionsByItem_shouldReturnAuctionsForItem() {
    List<Auction> result = queryService.getAuctionsByItem(item.getId());
    assertFalse(result.isEmpty());
    result.forEach(a -> assertEquals(item.getId(), a.getItemId()));
  }

  @Test
  void getAuctionsByItem_shouldReturnEmpty_whenNoAuction() {
    // Item chua co phien dau gia nao
    Item noAuctionItem =
        itemDAO.save(TestFixtures.item(seller.getId(), "No Auction", ItemType.ART));
    List<Auction> result = queryService.getAuctionsByItem(noAuctionItem.getId());
    assertTrue(result.isEmpty());
  }

  @Test
  void getHistoryAuctions_asBidder_shouldReturnAuctionWhereBidderParticipated() {
    // Tao phien FINISHED va them bid cua bidder
    Item item4 = itemDAO.save(TestFixtures.item(seller.getId(), "Bidder Auction", ItemType.ART));
    Auction temp4 =
        TestFixtures.auction(item4.getId(), seller.getId(), LocalDateTime.now().plusHours(2), 500L);
    temp4.setStartTime(LocalDateTime.now().plusHours(1));
    // Dung bien final de dung trong lambda
    final Auction savedBiddedAuction = auctionDAO.save(temp4);
    transactionManager.runWithoutResult(
        conn -> bidDAO.insertBid(conn, savedBiddedAuction.getId(), bidder.getId(), 1000L, false));
    savedBiddedAuction.setStatus(AuctionStatus.FINISHED);
    auctionDAO.update(savedBiddedAuction);

    List<Auction> history = queryService.getHistoryAuctions(bidder.getId());
    assertTrue(history.stream().anyMatch(a -> a.getId() == savedBiddedAuction.getId()));
  }
}
