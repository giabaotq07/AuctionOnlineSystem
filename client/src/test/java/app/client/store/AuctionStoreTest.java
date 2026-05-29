package app.client.store;

import static org.junit.jupiter.api.Assertions.*;

import app.common.dto.AuctionPreview;
import app.common.dto.UserPreview;
import app.common.enums.AuctionStatus;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
import app.common.models.Auction;
import app.common.models.Item;
import app.common.models.ItemFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Lop kiem thu cho AuctionStore phia Client. Viet bang tieng Viet khong dau de mentor de dang giai
 * thich.
 */
public class AuctionStoreTest {

  /**
   * Test toan bo cac chuc nang cua AuctionStore bao gom preview, detail, version merge va history.
   */
  @Test
  public void testAuctionStoreEverything() {
    AuctionStore store = AuctionStore.getInstance();
    assertNotNull(store);

    // Xoa sach lich su truoc khi kiem thu
    store.clearHistory();
    int auctionId = 10001;
    int itemId = 10010;
    int historyAuctionId = 10002;
    int appendedHistoryAuctionId = 10003;

    // 1. Test addPreview & getPreview
    LocalDateTime now = LocalDateTime.now();
    UserPreview mockSeller = new UserPreview(2, "seller_a", "seller_a", UserRole.SELLER, null);
    AuctionPreview preview1 =
        new AuctionPreview(
            auctionId,
            itemId,
            "Vat pham A",
            null,
            ItemType.ART,
            AuctionStatus.RUNNING,
            now,
            now.plusHours(2),
            10000L,
            10000L,
            1000L,
            1,
            mockSeller);
    store.addPreview(preview1);

    AuctionPreview cachedPreview = store.getPreview(auctionId);
    assertNotNull(cachedPreview);
    assertEquals("Vat pham A", cachedPreview.itemName());
    assertEquals(10000L, cachedPreview.highestBid());

    // 2. Test mergePreview (version moi hon phai de version cu)
    AuctionPreview previewNewVersion =
        new AuctionPreview(
            auctionId,
            itemId,
            "Vat pham A - Cap nhat",
            null,
            ItemType.ART,
            AuctionStatus.RUNNING,
            now,
            now.plusHours(2),
            12000L,
            10000L,
            1000L,
            2,
            mockSeller);
    store.addPreview(previewNewVersion);
    assertEquals("Vat pham A - Cap nhat", store.getPreview(auctionId).itemName());
    assertEquals(12000L, store.getPreview(auctionId).highestBid());

    // Neu version cu hon thi phai giu nguyen version moi hon
    AuctionPreview previewOldVersion =
        new AuctionPreview(
            auctionId,
            itemId,
            "Vat pham A - Cu",
            null,
            ItemType.ART,
            AuctionStatus.RUNNING,
            now,
            now.plusHours(2),
            5000L,
            10000L,
            1000L,
            0,
            mockSeller);
    store.addPreview(previewOldVersion);
    assertEquals("Vat pham A - Cap nhat", store.getPreview(auctionId).itemName());

    // 3. Test addDetail & getDetailIfLoaded
    Auction auction =
        new Auction(
            auctionId,
            itemId,
            2,
            3,
            AuctionStatus.RUNNING,
            now,
            now.plusHours(2),
            12000L,
            1,
            2,
            now,
            now);
    Item item =
        ItemFactory.createItem(
            itemId, "Vat pham A - Cap nhat", 2, "Mota", 10000L, 1000L, ItemType.ART);
    auction.setItem(item);
    store.addDetail(auction);

    assertTrue(store.hasDetail(auctionId));
    assertEquals(2, store.getKnownDetailVersion(auctionId));
    assertEquals(auction, store.getDetailIfLoaded(auctionId));
    assertEquals(auction, store.getAuction(auctionId));

    // Kiem tra item da duoc tu dong them vao ItemStore chua
    assertNotNull(ItemStore.getInstance().getItem(itemId));

    // 4. Test updateBid
    store.updateBid(auctionId, 15000L, 3L);
    assertEquals(15000L, store.getPreview(auctionId).highestBid());
    assertEquals(15000L, store.getDetailIfLoaded(auctionId).getHighestBid());
    assertEquals(3, store.getDetailIfLoaded(auctionId).getWinnerId());

    // 5. Test markCanceled
    store.markCanceled(auctionId);
    assertEquals(AuctionStatus.CANCELED, store.getPreview(auctionId).status());
    assertEquals(AuctionStatus.CANCELED, store.getDetailIfLoaded(auctionId).getStatus());

    // 6. Test markFinished
    store.markFinished(auctionId, 18000L, 5);
    assertEquals(AuctionStatus.FINISHED, store.getPreview(auctionId).status());
    assertEquals(18000L, store.getPreview(auctionId).highestBid());
    assertEquals(AuctionStatus.FINISHED, store.getDetailIfLoaded(auctionId).getStatus());
    assertEquals(18000L, store.getDetailIfLoaded(auctionId).getHighestBid());
    assertEquals(5, store.getDetailIfLoaded(auctionId).getWinnerId());

    // 7. Test History Auctions
    List<AuctionPreview> history = new ArrayList<>();
    UserPreview mockSellerB = new UserPreview(2, "seller_b", "seller_b", UserRole.SELLER, null);
    AuctionPreview preview2 =
        new AuctionPreview(
            historyAuctionId,
            20,
            "Vat pham B",
            null,
            ItemType.ART,
            AuctionStatus.FINISHED,
            now,
            now.plusHours(1),
            20000L,
            20000L,
            2000L,
            3,
            mockSellerB);
    history.add(previewNewVersion); // version=2
    history.add(preview2); // version=3

    store.setHistoryAuctions(history);
    List<AuctionPreview> storedHistory = store.getHistoryAuctionPreviews();
    assertEquals(2, storedHistory.size());
    assertEquals(3, store.getMaxHistoryVersion());

    // Append history
    UserPreview mockSellerC = new UserPreview(2, "seller_c", "seller_c", UserRole.SELLER, null);
    AuctionPreview preview3 =
        new AuctionPreview(
            appendedHistoryAuctionId,
            30,
            "Vat pham C",
            null,
            ItemType.ART,
            AuctionStatus.FINISHED,
            now,
            now.plusHours(3),
            30000L,
            30000L,
            3000L,
            5,
            mockSellerC);
    store.appendHistoryAuctions(Collections.singletonList(preview3));
    assertEquals(3, store.getHistoryAuctionPreviews().size());
    assertEquals(5, store.getMaxHistoryVersion());

    // Clear history
    store.clearHistory();
    assertTrue(store.getHistoryAuctionPreviews().isEmpty());
    assertEquals(-1, store.getMaxHistoryVersion());
  }
}
