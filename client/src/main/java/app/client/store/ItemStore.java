package app.client.store;

import app.common.models.Item;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ItemStore {
  private static volatile ItemStore instance;

  private final Map<Integer, Item> itemMap = new ConcurrentHashMap<>();

  private final Map<Integer, String> imageBase64Cache = new ConcurrentHashMap<>();

  private ItemStore() {}

  /** getInstance. */
  public static ItemStore getInstance() {
    if (instance == null) {
      synchronized (ItemStore.class) {
        if (instance == null) {
          instance = new ItemStore();
        }
      }
    }
    return instance;
  }

  public void addItem(Item item) {
    if (item == null) {
      return;
    }
    this.itemMap.put(item.getId(), item);
  }

  public Item getItem(int itemId) {
    return this.itemMap.get(itemId);
  }

  /**
   * Tìm item theo ID, trả về Optional để caller xử lý trường hợp không tìm thấy. Dùng bởi
   * UploadImageCommand (client) sau khi server phản hồi UPLOAD_IMAGE.
   */
  public Optional<Item> findById(int itemId) {
    return Optional.ofNullable(this.itemMap.get(itemId));
  }

  /**
   * Lấy tất cả item thuộc về một seller. Dùng bởi AuctionController để tìm item vừa tạo (lấy ID cao
   * nhất) trước khi upload ảnh.
   */
  public List<Item> getItemsBySeller(int sellerId) {
    return this.itemMap.values().stream()
        .filter(item -> item.getSellerId() == sellerId)
        .collect(Collectors.toList());
  }

  public void setItemImageBase64(int itemId, String base64Data) {
    if (base64Data != null && !base64Data.isBlank()) {
      this.imageBase64Cache.put(itemId, base64Data);
    }
  }

  public Optional<String> getItemImageBase64(int itemId) {
    return Optional.ofNullable(this.imageBase64Cache.get(itemId));
  }
}
