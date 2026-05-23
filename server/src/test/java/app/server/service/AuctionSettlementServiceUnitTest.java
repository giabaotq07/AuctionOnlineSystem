package app.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.common.enums.AuctionStatus;
import app.common.enums.UserRole;
import app.common.exception.DatabaseException;
import app.common.models.Auction;
import app.common.models.Bid;
import app.common.models.User;
import app.server.dao.BidDAO;
import app.server.dao.UserDAO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * AuctionSettlementServiceUnitTest. Kiem thu don vi logic quyet toan bang Mockito nham dam bao do
 * bao phu cac truong hop loi (User/Seller not found) mot cach doc lap, tin cay. Hoan toan dung doi
 * tuong User/Bid thuc te de tranh loi mock class cua Mockito tren Java 25.
 */
public class AuctionSettlementServiceUnitTest {

  @Test
  public void testUserNotFoundThrowsDatabaseException() {
    BidDAO mockBidDAO = mock(BidDAO.class);
    UserDAO mockUserDAO = mock(UserDAO.class);

    AuctionSettlementService service = new AuctionSettlementService(mockBidDAO, mockUserDAO);

    Auction auction = new Auction(10, 1, LocalDateTime.now().plusDays(1), 500L);
    auction.setId(123);

    // Dung doi tuong Bid thuc te thay vi Mock de dam bao tuong thich 100% JDK 25
    List<Bid> bids = new ArrayList<>();
    Bid realBid = new Bid(1, 123, 9999, "transient_bidder", 600L, LocalDateTime.now(), false);
    bids.add(realBid);

    // Dung any() de tranh phai mock lop Connection
    when(mockBidDAO.findByAuction(any(), eq(123))).thenReturn(bids);

    // Gia lap lockRow nem loi vi nguoi dung khong ton tai
    doThrow(new DatabaseException("Người dùng không tồn tại: 9999"))
        .when(mockUserDAO)
        .lockRow(any(), eq(9999));

    assertThrows(DatabaseException.class, () -> service.settleWalletsWithResult(null, auction));
    assertThrows(DatabaseException.class, () -> service.releaseWallets(null, auction));
  }

  @Test
  public void testSellerNotFoundThrowsDatabaseException() {
    BidDAO mockBidDAO = mock(BidDAO.class);
    UserDAO mockUserDAO = mock(UserDAO.class);

    AuctionSettlementService service = new AuctionSettlementService(mockBidDAO, mockUserDAO);

    // Phien dau gia co sellerId = 8888, winnerId = 2
    Auction auction = new Auction(10, 8888, LocalDateTime.now().plusDays(1), 500L);
    auction.setId(123);
    auction.setWinnerId(2);
    auction.setStatus(AuctionStatus.FINISHED);

    // Gia lap bid hop le tu winner thuc te
    List<Bid> bids = new ArrayList<>();
    Bid realBid = new Bid(1, 123, 2, "winner", 600L, LocalDateTime.now(), false);
    bids.add(realBid);

    when(mockBidDAO.findByAuction(any(), eq(123))).thenReturn(bids);

    // Winner dung model thuc te, khong mock tren JDK 25
    User winnerUser = app.TestFixtures.user("winner", UserRole.BIDDER, BigDecimal.valueOf(1000));
    winnerUser.setId(2);
    winnerUser.getWallet().setFrozenAmount("123", BigDecimal.valueOf(600));

    when(mockUserDAO.findById(any(), eq(2))).thenReturn(java.util.Optional.of(winnerUser));

    // Gia lap lockRow cho seller 8888 nem loi vi khong ton tai
    doThrow(new DatabaseException("Người dùng không tồn tại: 8888"))
        .when(mockUserDAO)
        .lockRow(any(), eq(8888));

    assertThrows(DatabaseException.class, () -> service.settleWalletsWithResult(null, auction));
  }
}
