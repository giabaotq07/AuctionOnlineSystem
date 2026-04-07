package Common;

import java.util.*;
import java.time.LocalDateTime;

public class Auction {
    private static volatile Auction instance;
    private final List<Item> itemList = new ArrayList<>();

    private Auction() {}

    public static Auction getInstance() {
        if (instance == null) {
            synchronized (Auction.class) {
                if (instance == null) {
                    instance = new Auction();
                }
            }
        }
        return instance;
    }

    // ===================== ADD ITEM =====================
    public void addItem(String name, String category, double startPrice, int minutes) {
        LocalDateTime endTime = LocalDateTime.now().plusMinutes(minutes);
        Item item = new Item(name, category, startPrice, endTime);
        itemList.add(item);
    }

    // ===================== BID =====================
    public Bid.BidResult bidItem(int id, String bidderName, double amount) {
        if (id < 0 || id >= itemList.size()) {
            return new Bid.BidResult(false, "ID không hợp lệ!");
        }

        return Bid.getInstance().placeBid(itemList.get(id), amount, bidderName);
    }

    // ===================== GET LIST =====================
    public List<Item> getItems() {
        return Collections.unmodifiableList(itemList);
    }

    // ===================== GET 1 ITEM =====================
    public Item getItem(int id) {
        if (id < 0 || id >= itemList.size()) {
            return null;
        }
        return itemList.get(id);
    }

    // ===================== FORMAT LIST (CHO UI) =====================
    public String getItemsAsString() {
        if (itemList.isEmpty()) {
            return "Chưa có mặt hàng nào.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("--- DANH SÁCH MẶT HÀNG ---\n");

        for (int i = 0; i < itemList.size(); i++) {
            sb.append("ID: ").append(i).append(" | ");
            sb.append(itemList.get(i).toString()).append("\n");
        }

        return sb.toString();
    }

    // ===================== CLEAR (OPTIONAL) =====================
    public void clearAll() {
        itemList.clear();
    }
}