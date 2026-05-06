package app.service;

import app.dao.AuctionDAO;
import app.dao.AutoBidDAO;
import app.dao.BidDAO;
import app.enums.AuctionStatus;
import app.exception.ServiceException;
import app.models.Auction;
import app.models.BidTransaction;
import java.util.List;
import java.util.Optional;

/**
 * Business logic layer cho chức năng đấu giá.
 *
 * <p>Luồng đặt giá thủ công (placeBid): 1. Validate phiên còn RUNNING 2. Validate giá hợp lệ (>
 * currentPrice + minIncrement) 3. INSERT bid trong transaction 4. Trigger auto-bid của các đối thủ
 * nếu có 5. Notify observer (realtime update)
 *
 * <p>Luồng auto-bid (triggerAutoBids): - Lấy tất cả AutoBidConfig của phiên (trừ người vừa bid) -
 * Sắp xếp ưu tiên: maxBid DESC, registeredAt ASC - Lần lượt thử đặt giá tự động cho đến khi không
 * ai có thể vượt
 */
public class BidService {

  // Bước giá tối thiểu mặc định (có thể đưa vào config/DB sau)
  private static final long DEFAULT_MIN_INCREMENT = 1L;

  // Anti-sniping: nếu bid trong X giây cuối → gia hạn thêm Y giây
  private static final int ANTI_SNIPE_THRESHOLD_SECONDS = 30;
  private static final int ANTI_SNIPE_EXTENSION_SECONDS = 60;

  private final BidDAO bidDAO;
  private final AutoBidDAO autoBidDAO;
  private final AuctionDAO sessionDAO;
  private final BidObserverService observerService;

  public BidService(
      BidDAO bidDAO,
      AutoBidDAO autoBidDAO,
      AuctionDAO sessionDAO,
      BidObserverService observerService) {
    this.bidDAO = bidDAO;
    this.autoBidDAO = autoBidDAO;
    this.sessionDAO = sessionDAO;
    this.observerService = observerService;
  }

  // ── 1. Đặt giá thủ công ──────────────────────────────────────────────────

  public void placeBid(int sessionId, int userId, long bidAmount) {
    bidDAO.placeBidAtomic(sessionId, userId, bidAmount, DEFAULT_MIN_INCREMENT);

    // Notify ngoài transaction (không ảnh hưởng tính toàn vẹn dữ liệu)
    observerService.notifyBidUpdated(sessionId);

    // Trigger auto-bid của các đối thủ
    //    triggerAutoBids(sessionId, userId, bidAmount);
  }

  // ── 2. Đăng ký / huỷ Auto-Bid ────────────────────────────────────────────

  // ── 3. Truy vấn lịch sử bid ───────────────────────────────────────────────

  /** Trả về danh sách bid theo giá giảm dần (dùng cho bảng lịch sử). */
  public List<BidTransaction> getBidHistory(int sessionId) {
    return bidDAO.findBySession(sessionId);
  }

  /** Trả về danh sách bid theo thời gian tăng dần (dùng cho line chart). */
  public List<BidTransaction> getBidHistoryForChart(int sessionId) {
    return bidDAO.findBySessionForChart(sessionId);
  }

  /** Trả về bid cao nhất hiện tại của phiên (Optional.empty nếu chưa có bid). */
  public Optional<BidTransaction> getHighestBid(int sessionId) {
    return bidDAO.findHighestBid(sessionId);
  }

  // ── Private: Auto-Bid Engine ──────────────────────────────────────────────

  // ── Private: Anti-Snipe ───────────────────────────────────────────────────

  // ── Private: Validators ───────────────────────────────────────────────────

  private Auction requireRunningSession(int sessionId) {
    Auction session =
        sessionDAO
            .findById(sessionId)
            .orElseThrow(() -> new ServiceException("Phiên đấu giá không tồn tại."));

    if (session.getStatus() != AuctionStatus.RUNNING) {
      throw new ServiceException(
          "Phiên đấu giá đã " + session.getStatus().name().toLowerCase() + ".");
    }
    return session;
  }

  private void validateBidAmount(long bidAmount, long currentPrice) {
    if (bidAmount < currentPrice + DEFAULT_MIN_INCREMENT) {
      throw new ServiceException(
          "Giá đặt phải cao hơn giá hiện tại ít nhất "
              + DEFAULT_MIN_INCREMENT
              + " VNĐ. "
              + "Giá tối thiểu: "
              + (currentPrice + DEFAULT_MIN_INCREMENT));
    }
  }

  /** Lấy giá hiện tại của phiên (dùng nội bộ). */
  private long getCurrentPrice(int sessionId) {
    return sessionDAO.findById(sessionId).map(Auction::getHighestBid).orElse(0L);
  }
}
