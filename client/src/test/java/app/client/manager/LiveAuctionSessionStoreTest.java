package app.client.manager;

import static org.junit.jupiter.api.Assertions.*;

import app.client.store.LiveAuctionSessionStore;
import app.common.dto.AuctionPreview;
import app.common.dto.UserPreview;
import app.common.enums.AuctionStatus;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Lop kiem thu cho LiveAuctionSessionStore phia Client. Viet bang tieng Viet khong dau de mentor de
 * dang giai thich.
 */
public class LiveAuctionSessionStoreTest {

  /** Test toan bo cac chuc nang chon phien, lay proxy va clear phien quan ly cua live screen. */
  @Test
  public void testLiveAuctionSessionStore() {
    LiveAuctionSessionStore store = LiveAuctionSessionStore.getInstance();
    assertNotNull(store);

    // Kiem tra singleton
    assertEquals(store, LiveAuctionSessionStore.getInstance());

    // Clean phien truoc khi test
    store.clear();
    assertNull(store.getSelectedAuctionId());
    assertNull(store.getSelectedProxy());

    // 1. Chon phien bang Preview
    LocalDateTime now = LocalDateTime.now();
    UserPreview mockSeller = new UserPreview(2, "seller", "seller", UserRole.SELLER, null);
    AuctionPreview preview =
        new AuctionPreview(
            15,
            100,
            "Vat pham 15",
            null,
            ItemType.VEHICLE,
            AuctionStatus.RUNNING,
            now,
            now.plusHours(1),
            50000L,
            10000L,
            1000L,
            2,
            mockSeller);

    // Select phien preview null thi khong thay doi gi
    store.selectAuction((AuctionPreview) null);
    assertNull(store.getSelectedAuctionId());

    store.selectAuction(preview);
    assertEquals(15, store.getSelectedAuctionId());
    assertNotNull(store.getSelectedProxy());
    assertEquals(15, store.getSelectedProxy().getAuctionId());

    // 2. Chon phien bang ID truc tiep
    store.selectAuction(25);
    assertEquals(25, store.getSelectedAuctionId());
    assertEquals(25, store.getSelectedProxy().getAuctionId());

    // Select so am thi khong thay doi
    store.selectAuction(-10);
    assertEquals(25, store.getSelectedAuctionId());

    // 3. Test finishDetailRequest (xem no co hoat dong dung phien khong)
    // De test phan nay ta can set proxy request in flight
    // Vi chung ta goi finish thi proxy hoan thanh in flight
    store.finishDetailRequest(25);
    assertFalse(store.getSelectedProxy().isRequestInFlight());

    // 4. Test clear
    store.clear();
    assertNull(store.getSelectedAuctionId());
  }
}
