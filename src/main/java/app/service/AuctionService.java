package app.service;

import app.config.DatabaseConnection;
import app.dao.AuctionDAO;
import app.dao.BidDAO;
import app.enums.AuctionStatus;
import app.exception.DatabaseException;
import app.exception.ServiceException;
import app.models.Auction;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class AuctionService {
  private final AuctionDAO auctionDAO;
  private final BidDAO bidDAO; // inject thẳng, bỏ BidService param

  public AuctionService(AuctionDAO auctionDAO, BidDAO bidDAO) {
    this.auctionDAO = auctionDAO;
    this.bidDAO = bidDAO;
  }

  // Trả Auction thẳng — đã throw nếu không tìm thấy
  public Auction getAuctionById(int id) {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      return getAuctionById(conn, id);
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối khi tải phiên đấu giá.", e);
    }
  }

  public Auction createAuction(Auction auction) {
    if (auction.getEndTime().isBefore(LocalDateTime.now())) {
      throw new ServiceException("Thời gian kết thúc không thể ở quá khứ.");
    }
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      return auctionDAO.save(conn, auction);
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối khi tạo phiên đấu giá.", e);
    }
  }

  public List<Auction> getAllAuctions() {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      return auctionDAO.findAll(conn);
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối khi tải danh sách phiên đấu giá.", e);
    }
  }

  public boolean closeIfExpired(int auctionId) {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      Auction auction = getAuctionById(conn, auctionId);
      auction.getLock().lock();
      try {
        if (!auction.isExpired()) return false;

        AuctionStatus s = auction.getStatus();
        if (s != AuctionStatus.OPEN && s != AuctionStatus.RUNNING) return false;

        auction.finish();
        auctionDAO.updateStatus(conn, auctionId, AuctionStatus.FINISHED);
        return true;

      } finally {
        auction.getLock().unlock();
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối khi đóng phiên đấu giá.", e);
    }
  }

  public void handleCompletion(int auctionId) {
    if (!closeIfExpired(auctionId)) return;
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      bidDAO
          .findHighestBid(conn, auctionId)
          .ifPresentOrElse(
              bid -> {
                auctionDAO.updateWinner(conn, auctionId, bid.getBidderId());
                System.out.printf(
                    "Phiên %d kết thúc. Winner: %s, Giá: %,d%n",
                    auctionId, bid.getBidderName(), bid.getAmount());
              },
              () -> System.out.println("Phiên " + auctionId + " kết thúc. Không có bid."));
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi xử lý kết thúc phiên.", e);
    }
  }

  public void updateStatus(int auctionId, AuctionStatus status) {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      boolean ok = auctionDAO.updateStatus(conn, auctionId, status);
      if (!ok) throw new ServiceException("Không thể cập nhật trạng thái phiên: " + auctionId);
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối khi cập nhật trạng thái phiên.", e);
    }
  }

  public void setStartTime(int auctionId, LocalDateTime startTime) {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      auctionDAO.updateStartTime(conn, auctionId, startTime);
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối khi cập nhật thời gian bắt đầu phiên.", e);
    }
  }

  public void setEndTime(int auctionId, LocalDateTime endTime) {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      auctionDAO.updateEndTime(conn, auctionId, endTime);
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối khi cập nhật thời gian bắt đầu phiên.", e);
    }
  }

  private Auction getAuctionById(Connection conn, int id) {
    return auctionDAO
        .findById(conn, id)
        .orElseThrow(() -> new ServiceException("Không tìm thấy phiên ID: " + id));
  }
}
