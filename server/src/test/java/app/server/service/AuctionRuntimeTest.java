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
import app.server.dao.*;
import app.server.dao.impl.*;
import app.server.database.TransactionManager;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * AuctionRuntimeTest. Kịch bản kiểm thử runtime của 1 phiên đấu giá có sử dụng Autobid theo mô hình
 * AAA (Arrange-Act-Assert).
 */
public class AuctionRuntimeTest extends app.server.dao.BaseDAOTest {

  private UserDAO userDAO;
  private ItemDAO itemDAO;
  private AuctionDAO auctionDAO;
  private BidDAO bidDAO;
  private AutoBidDAO autoBidDAO;
  private TransactionManager transactionManager;

  private AutoBidService autoBidService;
  private BidService bidService;
  private AuctionSettlementService settlementService;

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
    // Khởi tạo SettlementService với AutoBidDAO để giải phóng/quyết toán ví chính xác khi có
    // autobid
    settlementService = new AuctionSettlementService(bidDAO, userDAO, autoBidDAO);
  }

  @Test
  void testAuctionSessionWithAutoBidAAA() {
    // ==========================================
    // 1. ARRANGE (Chuẩn bị)
    // ==========================================
    // Xác minh quy tắc đường dẫn tương thích (Strict Constraints)
    Path userDir = Paths.get(System.getProperty("user.dir"));
    assertNotNull(userDir, "Đường dẫn thư mục chạy ứng dụng phải hợp lệ");

    // Tạo tài khoản người bán và những người tham gia đấu giá
    User seller =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("seller"), UserRole.SELLER, BigDecimal.valueOf(1000)));
    User bidderA =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidderA"), UserRole.BIDDER, BigDecimal.valueOf(5000)));
    User bidderB =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidderB"), UserRole.BIDDER, BigDecimal.valueOf(5000)));
    User bidderC =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidderC"), UserRole.BIDDER, BigDecimal.valueOf(100)));

    // Tạo vật phẩm với giá khởi điểm 1000 và bước giá tối thiểu (step price) là 100
    Item item =
        itemDAO.save(TestFixtures.item(seller.getId(), "Bức tranh Mona Lisa cổ", ItemType.ART));

    // Khởi tạo phiên đấu giá
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusDays(1), 1000L));

    // Kích hoạt phiên đấu giá sang trạng thái RUNNING để chấp nhận đặt giá
    auction.start();
    auctionDAO.update(auction);

    // ==========================================
    // 2. ACT (Thực thi)
    // ==========================================
    // Người A cấu hình AutoBid: giá tối đa 3000, bước tăng 100
    autoBidService.setAutoBid(auction.getId(), bidderA, 3000L, 100L);

    // Người B cấu hình AutoBid: giá tối đa 2500, bước tăng 100
    autoBidService.setAutoBid(auction.getId(), bidderB, 2500L, 100L);

    // Người C thử đặt giá thủ công không hợp lệ hoặc số dư ví không đủ
    // Trình hợp lệ hóa giá trị bid sẽ từ chối giá đặt quá thấp (ví dụ: 800L nhỏ hơn giá khởi điểm)
    assertThrows(
        ServiceException.class,
        () -> {
          bidService.placeBid(auction.getId(), bidderC, 800L);
        },
        "Người C đặt giá thấp hơn giá khởi điểm phải bị từ chối.");

    // Người B đặt giá thủ công là 1200L để kích hoạt cuộc đua Autobid tự động
    // Cuộc đua Autobid giữa Người A (tối đa 3000) và Người B (tối đa 2500)
    // Giá sẽ tự động nâng lên cho đến khi Người B đạt mức giới hạn 2500, và Người A tự động dẫn
    // trước
    // với mức giá tiếp theo: 2500 (tối đa của B) + 100 (bước giá) = 2600.
    Auction updatedAuction = bidService.placeBid(auction.getId(), bidderB, 1200L);

    // Đóng phiên đấu giá (Chuyển trạng thái sang FINISHED)
    updatedAuction.setStatus(AuctionStatus.FINISHED);
    auctionDAO.update(updatedAuction);

    // Tiến hành quyết toán tiền ví (Settlement)
    transactionManager.runWithoutResult(
        conn -> {
          settlementService.settleWallets(conn, updatedAuction);
        });

    // ==========================================
    // 3. ASSERT (Xác nhận)
    // ==========================================
    // Truy vấn dữ liệu mới nhất từ cơ sở dữ liệu để kiểm tra kết quả
    Auction finalAuction =
        auctionDAO
            .findById(auction.getId())
            .orElseThrow(
                () -> new AssertionError("Không tìm thấy phiên đấu giá sau khi kết thúc."));
    User finalSeller = userDAO.findById(seller.getId()).orElseThrow();
    User finalBidderA = userDAO.findById(bidderA.getId()).orElseThrow();
    User finalBidderB = userDAO.findById(bidderB.getId()).orElseThrow();
    User finalBidderC = userDAO.findById(bidderC.getId()).orElseThrow();

    // Xác nhận trạng thái phiên đấu giá
    assertEquals(
        AuctionStatus.FINISHED, finalAuction.getStatus(), "Phiên đấu giá phải ở trạng thái đóng.");

    // Xác nhận người chiến thắng và giá chốt phiên
    assertEquals(
        bidderA.getId(),
        finalAuction.getWinnerId(),
        "Người thắng phải là Người A (do có autobid max cao nhất).");
    assertEquals(2600L, finalAuction.getHighestBid(), "Giá chốt phiên đấu giá phải là 2600.");

    // Xác nhận tài khoản ví của Người A (Người thắng):
    // Số dư ban đầu: 5000. Trừ đi giá chốt: 2600. Số dư khả dụng còn: 2400.
    // Số tiền đóng băng phải được giải phóng hoàn toàn về 0.
    assertEquals(
        BigDecimal.valueOf(2400).stripTrailingZeros(),
        finalBidderA.getWallet().getAvailableBalance().stripTrailingZeros(),
        "Số dư khả dụng của Người A phải là 2400.");
    assertEquals(
        BigDecimal.ZERO.stripTrailingZeros(),
        finalBidderA
            .getWallet()
            .getFrozenAmount(String.valueOf(auction.getId()))
            .stripTrailingZeros(),
        "Số tiền đóng băng của Người A phải giải phóng về 0.");

    // Xác nhận tài khoản ví của Người B (Người thua cuộc):
    // Được trả lại toàn bộ số tiền đã đóng băng. Số dư khả dụng trở lại 5000.
    assertEquals(
        BigDecimal.valueOf(5000).stripTrailingZeros(),
        finalBidderB.getWallet().getAvailableBalance().stripTrailingZeros(),
        "Số dư khả dụng của Người B phải được hoàn trả đủ 5000.");
    assertEquals(
        BigDecimal.ZERO.stripTrailingZeros(),
        finalBidderB
            .getWallet()
            .getFrozenAmount(String.valueOf(auction.getId()))
            .stripTrailingZeros(),
        "Số tiền đóng băng của Người B phải giải phóng về 0.");

    // Xác nhận tài khoản ví của Người C (Không tham gia thành công):
    // Số dư khả dụng giữ nguyên 100.
    assertEquals(
        BigDecimal.valueOf(100).stripTrailingZeros(),
        finalBidderC.getWallet().getAvailableBalance().stripTrailingZeros(),
        "Số dư khả dụng của Người C phải giữ nguyên 100.");

    // Xác nhận tài khoản ví của Người bán (Seller):
    // Nhận được tiền bán sản phẩm: 1000 (ban đầu) + 2600 (giá chốt) = 3600.
    assertEquals(
        BigDecimal.valueOf(3600).stripTrailingZeros(),
        finalSeller.getWallet().getAvailableBalance().stripTrailingZeros(),
        "Người bán phải nhận được 2600, nâng tổng số dư lên 3600.");
  }
}
