package app.client.store;

import static org.junit.jupiter.api.Assertions.*;

import app.common.models.Bid;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Lop kiem thu cho BidStore phia Client. Viet bang tieng Viet khong dau de mentor de dang giai
 * thich.
 */
public class BidStoreTest {

  /** Test khoi tao singleton va them/lay bid tu cache. */
  @Test
  public void testBidStoreCaching() {
    BidStore store = BidStore.getInstance();
    assertNotNull(store);

    // Check singleton
    assertEquals(store, BidStore.getInstance());

    // Truong hop add bid null
    store.addBid(null);

    // Them bid hop le
    Bid bid = new Bid(1, 100, 2, "Bidder A", 1500L, LocalDateTime.now(), false);
    store.addBid(bid);

    Bid cached = store.getBid(1);
    assertNotNull(cached);
    assertEquals(100, cached.getAuctionId());
    assertEquals(2, cached.getBidderId());
    assertEquals("Bidder A", cached.getBidderName());
    assertEquals(1500L, cached.getAmount());

    // Lay bid khong ton tai
    assertNull(store.getBid(999));
  }
}
