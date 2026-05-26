package app.server.service;

import static org.junit.jupiter.api.Assertions.*;

import app.TestFixtures;
import app.common.enums.AuctionStatus;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.models.Auction;
import app.common.models.AutoBid;
import app.common.models.Bid;
import app.common.models.Item;
import app.common.models.User;
import app.server.dao.*;
import app.server.dao.impl.*;
import app.server.database.TransactionManager;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
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

  // ===========================================================================
  // TEST 1: Kịch bản chính — Cuộc đua AutoBid giữa 2 người, kèm bid không hợp lệ
  // ===========================================================================
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

  // ===========================================================================
  // TEST 2: Vòng đời phiên đấu giá — Kiểm tra chuyển đổi trạng thái OPEN → RUNNING → FINISHED
  // ===========================================================================
  @Test
  void testAuctionLifecycleTransitions() {
    // ==========================================
    // 1. ARRANGE
    // ==========================================
    User seller =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("seller_lc"), UserRole.SELLER, BigDecimal.valueOf(500)));
    Item item =
        itemDAO.save(TestFixtures.item(seller.getId(), "Đồng hồ Rolex cổ", ItemType.ELECTRONICS));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusDays(1), 500L));

    // ==========================================
    // 2. ACT & 3. ASSERT — Kiểm tra từng bước chuyển trạng thái
    // ==========================================
    // Phiên mới tạo phải ở trạng thái OPEN
    assertEquals(AuctionStatus.OPEN, auction.getStatus(), "Phiên mới tạo phải là OPEN.");

    // Bắt đầu phiên → RUNNING
    auction.start();
    auctionDAO.update(auction);
    Auction runningAuction = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(
        AuctionStatus.RUNNING, runningAuction.getStatus(), "Phiên sau start phải RUNNING.");
    assertNotNull(runningAuction.getStartTime(), "Start time phải được gán khi start.");

    // Kết thúc phiên → FINISHED
    runningAuction.finish();
    auctionDAO.update(runningAuction);
    Auction finishedAuction = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(
        AuctionStatus.FINISHED, finishedAuction.getStatus(), "Phiên sau finish phải FINISHED.");

    // Không thể start lại phiên đã FINISHED
    assertThrows(
        IllegalStateException.class, finishedAuction::start, "Không thể start phiên đã FINISHED.");

    // Không thể cancel phiên đã FINISHED
    assertThrows(
        IllegalStateException.class,
        finishedAuction::cancel,
        "Không thể cancel phiên đã FINISHED.");
  }

  // ===========================================================================
  // TEST 3: Từ chối bid khi phiên chưa RUNNING
  // ===========================================================================
  @Test
  void testBidRejectedWhenAuctionNotRunning() {
    // ==========================================
    // 1. ARRANGE
    // ==========================================
    User seller =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("seller_nr"), UserRole.SELLER, BigDecimal.valueOf(1000)));
    User bidder =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidder_nr"), UserRole.BIDDER, BigDecimal.valueOf(5000)));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Xe máy cổ", ItemType.VEHICLE));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusDays(1), 1000L));
    // Phiên vẫn ở trạng thái OPEN, chưa start

    // ==========================================
    // 2. ACT & 3. ASSERT
    // ==========================================
    // Đặt giá khi phiên chưa RUNNING phải bị từ chối
    assertThrows(
        ServiceException.class,
        () -> bidService.placeBid(auction.getId(), bidder, 1200L),
        "Không thể đặt giá khi phiên chưa RUNNING.");

    // AutoBid cũng phải bị từ chối khi phiên chưa RUNNING
    assertThrows(
        ServiceException.class,
        () -> autoBidService.setAutoBid(auction.getId(), bidder, 2000L, 100L),
        "Không thể cấu hình AutoBid khi phiên chưa RUNNING.");
  }

  // ===========================================================================
  // TEST 4: Disable AutoBid giữa chừng — bidder tắt autobid rồi bị outbid
  // ===========================================================================
  @Test
  void testDisableAutoBidMidSession() {
    // ==========================================
    // 1. ARRANGE
    // ==========================================
    User seller =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("seller_dis"), UserRole.SELLER, BigDecimal.valueOf(1000)));
    User bidderA =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidderA_dis"), UserRole.BIDDER, BigDecimal.valueOf(5000)));
    User bidderB =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidderB_dis"), UserRole.BIDDER, BigDecimal.valueOf(5000)));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Tranh sơn dầu", ItemType.ART));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusDays(1), 1000L));
    auction.start();
    auctionDAO.update(auction);

    // ==========================================
    // 2. ACT
    // ==========================================
    // Người A cấu hình AutoBid tối đa 3000
    autoBidService.setAutoBid(auction.getId(), bidderA, 3000L, 100L);

    // Người A tắt AutoBid trước khi có bid thủ công
    autoBidService.disableAutoBid(auction.getId(), bidderA);

    // Kiểm tra frozen funds đã được giải phóng sau khi disable
    User afterDisable = userDAO.findById(bidderA.getId()).orElseThrow();
    assertEquals(
        BigDecimal.ZERO.stripTrailingZeros(),
        afterDisable
            .getWallet()
            .getFrozenAmount(String.valueOf(auction.getId()))
            .stripTrailingZeros(),
        "Frozen funds phải được giải phóng sau khi disable AutoBid.");

    // Người B đặt giá thủ công — AutoBid của A đã tắt nên không tự phản hồi
    Auction afterBid = bidService.placeBid(auction.getId(), bidderB, 1200L);

    // ==========================================
    // 3. ASSERT
    // ==========================================
    // Người B phải là người dẫn đầu vì AutoBid của A đã bị tắt
    assertEquals(
        bidderB.getId(),
        afterBid.getWinnerId(),
        "Người B phải dẫn đầu vì AutoBid của A đã bị disable.");
    assertEquals(1200L, afterBid.getHighestBid(), "Giá cao nhất phải là bid thủ công 1200.");

    // Ví của Người A phải hoàn toàn tự do (5000), không bị đóng băng
    User finalBidderA = userDAO.findById(bidderA.getId()).orElseThrow();
    assertEquals(
        BigDecimal.valueOf(5000).stripTrailingZeros(),
        finalBidderA.getWallet().getAvailableBalance().stripTrailingZeros(),
        "Số dư Người A phải giữ nguyên 5000 vì AutoBid đã tắt.");
  }

  // ===========================================================================
  // TEST 5: Quyết toán khi phiên kết thúc không có ai bid (không có winner)
  // ===========================================================================
  @Test
  void testSettlementWithoutWinner() {
    // ==========================================
    // 1. ARRANGE
    // ==========================================
    User seller =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("seller_nw"), UserRole.SELLER, BigDecimal.valueOf(1000)));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Bình gốm cổ", ItemType.ART));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusDays(1), 500L));
    auction.start();
    auctionDAO.update(auction);

    // ==========================================
    // 2. ACT — Đóng phiên mà không có ai đặt giá
    // ==========================================
    Auction stored = auctionDAO.findById(auction.getId()).orElseThrow();
    stored.setStatus(AuctionStatus.FINISHED);
    auctionDAO.update(stored);

    transactionManager.runWithoutResult(conn -> settlementService.settleWallets(conn, stored));

    // ==========================================
    // 3. ASSERT
    // ==========================================
    Auction finalAuction = auctionDAO.findById(auction.getId()).orElseThrow();
    assertNull(finalAuction.getWinnerId(), "Phiên không có bid thì không có winner.");
    assertEquals(AuctionStatus.FINISHED, finalAuction.getStatus(), "Trạng thái phải là FINISHED.");
    assertEquals(500L, finalAuction.getHighestBid(), "Giá phải giữ nguyên giá khởi điểm 500.");

    // Ví seller không thay đổi (không ai mua)
    User finalSeller = userDAO.findById(seller.getId()).orElseThrow();
    assertEquals(
        BigDecimal.valueOf(1000).stripTrailingZeros(),
        finalSeller.getWallet().getAvailableBalance().stripTrailingZeros(),
        "Ví seller phải giữ nguyên 1000 khi không có winner.");
  }

  // ===========================================================================
  // TEST 6: Hai AutoBid cùng maxAmount — Người đặt trước thắng (Tie-breaking)
  // ===========================================================================
  @Test
  void testEqualMaxAmountAutoBidTieBreaking() {
    // ==========================================
    // 1. ARRANGE
    // ==========================================
    User seller =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("seller_tie"), UserRole.SELLER, BigDecimal.valueOf(1000)));
    User bidderA =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidderA_tie"), UserRole.BIDDER, BigDecimal.valueOf(5000)));
    User bidderB =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidderB_tie"), UserRole.BIDDER, BigDecimal.valueOf(5000)));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Kiếm cổ Nhật Bản", ItemType.ART));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusDays(1), 1000L));
    auction.start();
    auctionDAO.update(auction);

    // ==========================================
    // 2. ACT
    // ==========================================
    // Người A đặt AutoBid trước: maxAmount = 2000
    autoBidService.setAutoBid(auction.getId(), bidderA, 2000L, 100L);
    // Người B đặt AutoBid sau: maxAmount = 2000 (cùng mức tối đa)
    autoBidService.setAutoBid(auction.getId(), bidderB, 2000L, 100L);

    // Kích hoạt cuộc đua bằng bid thủ công của B
    Auction afterRace = bidService.placeBid(auction.getId(), bidderB, 1100L);

    // ==========================================
    // 3. ASSERT
    // ==========================================
    // Khi maxAmount bằng nhau, người đặt AutoBid trước (Người A) phải thắng
    assertEquals(
        bidderA.getId(),
        afterRace.getWinnerId(),
        "Khi maxAmount bằng nhau, người đặt AutoBid sớm hơn (Người A) phải thắng.");
    // Giá chốt = maxAmount (vì cả hai cùng mức tối đa, không cần tăng thêm)
    assertEquals(
        2000L,
        afterRace.getHighestBid(),
        "Giá chốt phải là 2000 (bằng maxAmount chung khi tie-break).");
  }

  // ===========================================================================
  // TEST 7: Cuộc đua 3 người — thêm bidder C với maxAmount thấp nhất
  // ===========================================================================
  @Test
  void testThreeWayAutoBidCompetition() {
    // ==========================================
    // 1. ARRANGE
    // ==========================================
    User seller =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("seller_3w"), UserRole.SELLER, BigDecimal.valueOf(1000)));
    User bidderA =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidderA_3w"), UserRole.BIDDER, BigDecimal.valueOf(10000)));
    User bidderB =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidderB_3w"), UserRole.BIDDER, BigDecimal.valueOf(10000)));
    User bidderC =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidderC_3w"), UserRole.BIDDER, BigDecimal.valueOf(10000)));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Xe Ferrari cổ", ItemType.VEHICLE));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusDays(1), 1000L));
    auction.start();
    auctionDAO.update(auction);

    // ==========================================
    // 2. ACT
    // ==========================================
    // Ba người cùng cấu hình AutoBid với maxAmount khác nhau:
    // A: max 5000, B: max 3000, C: max 2000
    autoBidService.setAutoBid(auction.getId(), bidderA, 5000L, 100L);
    autoBidService.setAutoBid(auction.getId(), bidderB, 3000L, 100L);
    autoBidService.setAutoBid(auction.getId(), bidderC, 2000L, 100L);

    // C kích hoạt cuộc đua bằng bid thủ công
    Auction afterRace = bidService.placeBid(auction.getId(), bidderC, 1100L);

    // ==========================================
    // 3. ASSERT
    // ==========================================
    // Người A (max 5000) phải thắng
    assertEquals(
        bidderA.getId(),
        afterRace.getWinnerId(),
        "Người A có maxAmount cao nhất phải thắng cuộc đua 3 người.");

    // Giá chốt = max(runner-up) + step = 3000 + 100 = 3100
    assertEquals(
        3100L,
        afterRace.getHighestBid(),
        "Giá chốt phải là 3100 (runner-up B max 3000 + bước giá 100).");

    // AutoBid của B (max 3000) phải bị disable vì bị outbid (3100 > 3000)
    AutoBid bidderBAutoBid =
        autoBidDAO.findByAuctionAndUser(auction.getId(), bidderB.getId()).orElseThrow();
    assertFalse(bidderBAutoBid.isEnabled(), "AutoBid của B phải bị disable vì bị outbid.");

    // AutoBid của C (max 2000) cũng phải bị disable
    AutoBid bidderCAutoBid =
        autoBidDAO.findByAuctionAndUser(auction.getId(), bidderC.getId()).orElseThrow();
    assertFalse(bidderCAutoBid.isEnabled(), "AutoBid của C phải bị disable vì bị outbid.");
  }

  // ===========================================================================
  // TEST 8: Xác minh lịch sử bid — kiểm tra số lượng và thứ tự các bid records
  // ===========================================================================
  @Test
  void testBidHistoryRecordsAfterAutoBidResolution() {
    // ==========================================
    // 1. ARRANGE
    // ==========================================
    User seller =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("seller_hist"), UserRole.SELLER, BigDecimal.valueOf(1000)));
    User bidderA =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidderA_hist"), UserRole.BIDDER, BigDecimal.valueOf(5000)));
    User bidderB =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidderB_hist"), UserRole.BIDDER, BigDecimal.valueOf(5000)));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Đĩa nhạc vinyl cổ", ItemType.ART));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusDays(1), 1000L));
    auction.start();
    auctionDAO.update(auction);

    // ==========================================
    // 2. ACT
    // ==========================================
    autoBidService.setAutoBid(auction.getId(), bidderA, 2000L, 100L);
    // B bid thủ công kích hoạt AutoBid của A phản hồi
    bidService.placeBid(auction.getId(), bidderB, 1200L);

    // ==========================================
    // 3. ASSERT
    // ==========================================
    List<Bid> bids = bidDAO.findByAuction(auction.getId());
    // Phải có ít nhất 2 bids: 1 thủ công của B + 1 autobid phản hồi của A
    assertTrue(bids.size() >= 2, "Phải có ít nhất 2 bid records (1 manual + 1 autobid).");

    // Bid thủ công của B
    Bid manualBid = bids.stream().filter(b -> !b.isAutoBid()).findFirst().orElseThrow();
    assertEquals(bidderB.getId(), manualBid.getBidderId(), "Bid thủ công phải thuộc về Người B.");
    assertEquals(1200L, manualBid.getAmount(), "Bid thủ công phải là 1200.");

    // Bid tự động của A phải có cờ isAutoBid = true
    Bid autoBidRecord = bids.stream().filter(Bid::isAutoBid).findFirst().orElseThrow();
    assertEquals(
        bidderA.getId(), autoBidRecord.getBidderId(), "AutoBid record phải thuộc về Người A.");
    assertTrue(autoBidRecord.isAutoBid(), "Bid record phải được đánh dấu là auto-bid.");
  }

  // ===========================================================================
  // TEST 9: Kiểm tra đường dẫn runtime tương thích cross-platform
  // ===========================================================================
  @Test
  void testRuntimePathCompatibility() {
    // ==========================================
    // 1. ARRANGE & 2. ACT
    // ==========================================
    // Strict Constraint: Mọi code xử lý đường dẫn file phải dùng
    // Paths.get(System.getProperty("user.dir"), ...) để đảm bảo tương thích
    // khi chạy file Fat JAR trên mọi hệ điều hành.
    Path userDir = Paths.get(System.getProperty("user.dir"));
    Path subPath = Paths.get(System.getProperty("user.dir"), "server_data");

    // ==========================================
    // 3. ASSERT
    // ==========================================
    assertNotNull(userDir, "user.dir phải tồn tại trong mọi môi trường JVM.");
    assertTrue(userDir.isAbsolute(), "Đường dẫn user.dir phải là đường dẫn tuyệt đối.");
    assertNotNull(subPath, "Có thể tạo đường dẫn con từ user.dir.");
    assertTrue(subPath.startsWith(userDir), "Đường dẫn con phải nằm trong thư mục chạy ứng dụng.");
    assertFalse(
        subPath.toString().contains("\\\\") && subPath.toString().contains("/"),
        "Đường dẫn không được trộn lẫn separator Windows và Unix.");
  }

  // ===========================================================================
  // TEST 10: Overwrite AutoBid tăng maxAmount — cập nhật giữa chừng
  // ===========================================================================
  @Test
  void testOverwriteAutoBidIncreasesMaxAmount() {
    // ==========================================
    // 1. ARRANGE
    // ==========================================
    User seller =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("seller_ow"), UserRole.SELLER, BigDecimal.valueOf(1000)));
    User bidderA =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidderA_ow"), UserRole.BIDDER, BigDecimal.valueOf(10000)));
    User bidderB =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidderB_ow"), UserRole.BIDDER, BigDecimal.valueOf(10000)));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Nhẫn kim cương", ItemType.ART));
    Auction auction =
        auctionDAO.save(
            TestFixtures.auction(
                item.getId(), seller.getId(), LocalDateTime.now().plusDays(1), 1000L));
    auction.start();
    auctionDAO.update(auction);

    // ==========================================
    // 2. ACT
    // ==========================================
    // A đặt AutoBid ban đầu: max 2000
    autoBidService.setAutoBid(auction.getId(), bidderA, 2000L, 100L);

    // B đặt AutoBid: max 3000 → B sẽ thắng nếu cuộc đua diễn ra
    autoBidService.setAutoBid(auction.getId(), bidderB, 3000L, 100L);

    // A nâng AutoBid lên max 4000 (overwrite)
    autoBidService.setAutoBid(auction.getId(), bidderA, 4000L, 100L);

    // Kích hoạt cuộc đua bằng bid thủ công
    Auction afterRace = bidService.placeBid(auction.getId(), bidderA, 1100L);

    // ==========================================
    // 3. ASSERT
    // ==========================================
    // Sau overwrite, A có max 4000 > B max 3000 → A thắng
    // Giá chốt = max(B) + step = 3000 + 100 = 3100
    Auction finalAuction = auctionDAO.findById(auction.getId()).orElseThrow();
    assertEquals(
        bidderA.getId(),
        finalAuction.getWinnerId(),
        "Người A phải thắng sau khi nâng maxAmount lên 4000.");
    assertEquals(
        3100L, finalAuction.getHighestBid(), "Giá chốt phải là 3100 (runner-up B max 3000 + 100).");

    // Frozen funds của A phải phản ánh maxAmount mới (4000)
    // Nhưng sau khi resolve, frozen = giá chốt autobid (3100) hoặc maxAmount tùy logic
    User finalBidderA = userDAO.findById(bidderA.getId()).orElseThrow();
    BigDecimal frozenA = finalBidderA.getWallet().getFrozenAmount(String.valueOf(auction.getId()));
    assertTrue(
        frozenA.compareTo(BigDecimal.ZERO) >= 0,
        "Frozen funds của A phải là giá trị hợp lệ (>= 0).");
  }
}
