package app.server.service;

import static org.junit.jupiter.api.Assertions.*;

import app.common.enums.AuctionStatus;
import app.common.enums.UserRole;
import app.common.exception.DatabaseException;
import app.common.models.Auction;
import app.common.models.Bid;
import app.common.models.User;
import app.server.dao.BidDAO;
import app.server.dao.UserDAO;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * AuctionSettlementServiceUnitTest. Kiem thu don vi logic quyet toan bang fake DAO de tranh phu
 * thuoc Mockito inline tren Java 25.
 */
public class AuctionSettlementServiceUnitTest {

  @Test
  public void testUserNotFoundThrowsDatabaseException() {
    FakeBidDAO bidDAO = new FakeBidDAO();
    FakeUserDAO userDAO = new FakeUserDAO(Map.of(), 9999);

    AuctionSettlementService service = new AuctionSettlementService(bidDAO, userDAO);

    Auction auction = new Auction(10, 1, LocalDateTime.now().plusDays(1), 500L);
    auction.setId(123);

    List<Bid> bids = new ArrayList<>();
    Bid realBid = new Bid(1, 123, 9999, "transient_bidder", 600L, LocalDateTime.now(), false);
    bids.add(realBid);
    bidDAO.bids = bids;

    assertThrows(DatabaseException.class, () -> service.settleWalletsWithResult(null, auction));
    assertThrows(DatabaseException.class, () -> service.releaseWallets(null, auction));
  }

  @Test
  public void testSellerNotFoundThrowsDatabaseException() {
    User winnerUser = app.TestFixtures.user("winner", UserRole.BIDDER, BigDecimal.valueOf(1000));
    winnerUser.setId(2);
    winnerUser.getWallet().setFrozenAmount("123", BigDecimal.valueOf(600));
    FakeBidDAO bidDAO = new FakeBidDAO();
    FakeUserDAO userDAO = new FakeUserDAO(Map.of(2, winnerUser), 8888);
    AuctionSettlementService service = new AuctionSettlementService(bidDAO, userDAO);

    Auction auction = new Auction(10, 8888, LocalDateTime.now().plusDays(1), 500L);
    auction.setId(123);
    auction.setWinnerId(2);
    auction.setStatus(AuctionStatus.FINISHED);

    List<Bid> bids = new ArrayList<>();
    Bid realBid = new Bid(1, 123, 2, "winner", 600L, LocalDateTime.now(), false);
    bids.add(realBid);
    bidDAO.bids = bids;

    assertThrows(DatabaseException.class, () -> service.settleWalletsWithResult(null, auction));
  }

  @Test
  public void testUserNotFoundThrowsServiceException() {
    FakeBidDAO bidDAO = new FakeBidDAO();
    FakeUserDAO userDAO = new FakeUserDAO(Map.of(), -999);

    AuctionSettlementService service = new AuctionSettlementService(bidDAO, userDAO);

    Auction auction = new Auction(10, 1, LocalDateTime.now().plusDays(1), 500L);
    auction.setId(123);

    List<Bid> bids = new ArrayList<>();
    Bid realBid = new Bid(1, 123, 9999, "transient_bidder", 600L, LocalDateTime.now(), false);
    bids.add(realBid);
    bidDAO.bids = bids;

    assertThrows(
        app.common.exception.ServiceException.class,
        () -> service.settleWalletsWithResult(null, auction));
    assertThrows(
        app.common.exception.ServiceException.class, () -> service.releaseWallets(null, auction));
  }

  @Test
  public void testSellerNotFoundThrowsServiceException() {
    User winnerUser = app.TestFixtures.user("winner", UserRole.BIDDER, BigDecimal.valueOf(1000));
    winnerUser.setId(2);
    winnerUser.getWallet().setFrozenAmount("123", BigDecimal.valueOf(600));
    FakeBidDAO bidDAO = new FakeBidDAO();
    FakeUserDAO userDAO = new FakeUserDAO(Map.of(2, winnerUser), -999);
    AuctionSettlementService service = new AuctionSettlementService(bidDAO, userDAO);

    Auction auction = new Auction(10, 8888, LocalDateTime.now().plusDays(1), 500L);
    auction.setId(123);
    auction.setWinnerId(2);
    auction.setStatus(AuctionStatus.FINISHED);

    List<Bid> bids = new ArrayList<>();
    Bid realBid = new Bid(1, 123, 2, "winner", 600L, LocalDateTime.now(), false);
    bids.add(realBid);
    bidDAO.bids = bids;

    assertThrows(
        app.common.exception.ServiceException.class,
        () -> service.settleWalletsWithResult(null, auction));
  }

  private static final class FakeBidDAO implements BidDAO {
    private List<Bid> bids = List.of();

    @Override
    public void insertBid(int auctionId, int userId, long bidAmount, boolean isAutoBid) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void insertBid(
        Connection conn, int auctionId, int userId, long bidAmount, boolean isAutoBid) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Bid> findHighestBid(int auctionId) {
      return bids.stream().findFirst();
    }

    @Override
    public Optional<Bid> findHighestBid(Connection conn, int auctionId) {
      return findHighestBid(auctionId);
    }

    @Override
    public List<Bid> findByAuction(int auctionId) {
      return bids;
    }

    @Override
    public List<Bid> findByAuction(Connection conn, int auctionId) {
      return bids;
    }

    @Override
    public List<Bid> findByAuctionOrderByTime(int auctionId) {
      return bids;
    }

    @Override
    public boolean existsByAuctionAndUser(int auctionId, int userId) {
      return false;
    }
  }

  private static final class FakeUserDAO implements UserDAO {
    private final Map<Integer, User> users;
    private final int missingLockId;

    private FakeUserDAO(Map<Integer, User> users, int missingLockId) {
      this.users = users;
      this.missingLockId = missingLockId;
    }

    @Override
    public Optional<User> findById(int id) {
      return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findById(Connection conn, int id) {
      return findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<User> findByUsername(Connection conn, String username) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<User> findAll() {
      throw new UnsupportedOperationException();
    }

    @Override
    public User save(User user) {
      throw new UnsupportedOperationException();
    }

    @Override
    public User save(Connection conn, User user) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void update(User user) {}

    @Override
    public void update(Connection conn, User user) {}

    @Override
    public void lockRow(Connection conn, int id) {
      if (id == missingLockId) {
        throw new DatabaseException("Người dùng không tồn tại: " + id);
      }
    }

    @Override
    public void deleteAll() {
      throw new UnsupportedOperationException();
    }
  }
}
