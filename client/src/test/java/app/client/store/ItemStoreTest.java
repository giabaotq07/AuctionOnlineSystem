package app.client.store;

import static org.junit.jupiter.api.Assertions.*;

import app.common.enums.ItemType;
import app.common.models.Item;
import app.common.models.ItemFactory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Lop kiem thu cho ItemStore phia Client. Viet bang tieng Viet khong dau de giai thich. */
public class ItemStoreTest {

  /** Test toan bo cac tinh nang luu tru item va cache anh base64. */
  @Test
  public void testItemStoreFeatures() {
    ItemStore store = ItemStore.getInstance();
    assertNotNull(store);

    // Singleton check
    assertEquals(store, ItemStore.getInstance());

    // Truong hop item null
    store.addItem(null);

    // Them item hop le
    Item item1 =
        ItemFactory.createItem(
            10, "Tranh Nghe Thuat", 1, "Tranh son mai", 5000L, 500L, ItemType.ART);
    Item item2 =
        ItemFactory.createItem(
            11, "Laptop Thinkpad", 1, "Laptop cu", 12000L, 1000L, ItemType.ELECTRONICS);

    store.addItem(item1);
    store.addItem(item2);

    // Kiem tra getItem va findById
    assertEquals(item1, store.getItem(10));
    Optional<Item> found = store.findById(11);
    assertTrue(found.isPresent());
    assertEquals(item2, found.get());

    // Tim item khong ton tai
    assertNull(store.getItem(999));
    assertFalse(store.findById(999).isPresent());

    // Kiem tra getItemsBySeller
    List<Item> sellerItems = store.getItemsBySeller(1);
    assertNotNull(sellerItems);
    assertEquals(2, sellerItems.size());
    assertTrue(sellerItems.contains(item1));
    assertTrue(sellerItems.contains(item2));

    // Kiem tra cache anh base64
    store.setItemImageBase64(10, "base64_encoded_image_data_here");

    // Them null hoac blank thi khong duoc de de phong loi du lieu rong
    store.setItemImageBase64(11, null);
    store.setItemImageBase64(11, "   ");

    Optional<String> imgData = store.getItemImageBase64(10);
    assertTrue(imgData.isPresent());
    assertEquals("base64_encoded_image_data_here", imgData.get());

    assertFalse(store.getItemImageBase64(11).isPresent());
  }
}
