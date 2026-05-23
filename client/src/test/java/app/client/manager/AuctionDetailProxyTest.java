package app.client.manager;

import static org.junit.jupiter.api.Assertions.*;

import app.client.store.AuctionStore;
import app.common.dto.AuctionPreview;
import app.common.dto.UserPreview;
import app.common.enums.AuctionStatus;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
import app.common.exception.ConnectException;
import app.common.models.Auction;
import java.io.IOException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Lop kiem thu cho AuctionDetailProxy phia Client. Viet bang tieng Viet khong dau de mentor de dang
 * giai thich.
 */
public class AuctionDetailProxyTest {

  /** Test toan bo cac trang thai va dieu kien phai refresh chi tiet cua virtual proxy. */
  @Test
  public void testAuctionDetailProxy() {
    // 1. Kiem tra khoi tao voi ID khong hop le
    assertThrows(IllegalArgumentException.class, () -> new AuctionDetailProxy(0));
    assertThrows(IllegalArgumentException.class, () -> new AuctionDetailProxy(-5));

    // Khoi tao dung
    AuctionDetailProxy proxy = new AuctionDetailProxy(99);
    assertEquals(99, proxy.getAuctionId());
    assertFalse(proxy.isRequestInFlight());

    // 2. Kiem tra khi chua co du lieu gi ca
    assertNull(proxy.getPreview());
    assertNull(proxy.getDetailIfLoaded());
    assertFalse(proxy.isDetailLoaded());
    assertTrue(proxy.needsDetailRefresh()); // Chua load detail thi luon can refresh

    // 3. Test check in-flight request
    proxy.finishRequest();
    assertFalse(proxy.isRequestInFlight());

    // 4. Test logic needsDetailRefresh khi da co preview va detail
    LocalDateTime now = LocalDateTime.now();
    UserPreview mockSeller = new UserPreview(2, "seller", "seller", UserRole.SELLER, null);
    AuctionPreview preview =
        new AuctionPreview(
            99,
            100,
            "Vat pham 99",
            null,
            ItemType.ART,
            AuctionStatus.RUNNING,
            now,
            now.plusHours(1),
            50000L,
            10000L,
            1000L,
            2,
            mockSeller);
    AuctionStore.getInstance().addPreview(preview);
    assertEquals(preview, proxy.getPreview());

    // Gia lap co detail version = 2 (tuong duong preview version) -> khong can refresh nua
    Auction auction =
        new Auction(
            99, 100, 2, 3, AuctionStatus.RUNNING, now, now.plusHours(1), 50000L, 0, 2, now, now);
    AuctionStore.getInstance().addDetail(auction);

    assertTrue(proxy.isDetailLoaded());
    assertEquals(auction, proxy.getDetailIfLoaded());
    assertFalse(proxy.needsDetailRefresh()); // Version khop nhau -> khong can refresh

    // Gia lap co preview version = 3 (moi hon detail version = 2) -> phai refresh
    AuctionPreview newerPreview =
        new AuctionPreview(
            99,
            100,
            "Vat pham 99",
            null,
            ItemType.ART,
            AuctionStatus.RUNNING,
            now,
            now.plusHours(1),
            50000L,
            10000L,
            1000L,
            3,
            mockSeller);
    AuctionStore.getInstance().addPreview(newerPreview);
    assertTrue(proxy.needsDetailRefresh());

    // 5. Test requestDetail khi khong can refresh thi phai return ngay ma khong throw loi gi
    // Ta set lai detail version bang cach load detail phien ban moi hon
    Auction newerAuction =
        new Auction(
            99, 100, 2, 3, AuctionStatus.RUNNING, now, now.plusHours(1), 55000L, 0, 3, now, now);
    AuctionStore.getInstance().addDetail(newerAuction);
    assertFalse(proxy.needsDetailRefresh());

    try {
      proxy.requestDetail(); // khong need refresh -> khong lam gi ca
    } catch (IOException e) {
      fail("Khong duoc nem loi khi khong can refresh");
    }

    // 6. Test requestDetail thuc su khi can refresh thi phai nem loi ConnectException do socket
    // chua connected
    // Ta update preview len version 4
    AuctionPreview newestPreview =
        new AuctionPreview(
            99,
            100,
            "Vat pham 99",
            null,
            ItemType.ART,
            AuctionStatus.RUNNING,
            now,
            now.plusHours(1),
            55000L,
            10000L,
            1000L,
            4,
            mockSeller);
    AuctionStore.getInstance().addPreview(newestPreview);
    assertTrue(proxy.needsDetailRefresh());

    assertThrows(
        ConnectException.class,
        () -> {
          proxy.requestDetail(); // phai nem ra ConnectException vi chua bat server socket
        });
  }
}
