package Common;

import java.util.*;

public class Auction {

    private Map<String, Item> items = new HashMap<>();
    private Map<String, Bidder> bidders = new HashMap<>();
    private Map<String, List<BidTransaction>> bids = new HashMap<>();


    public void addItem(Item item) {
        items.put(item.getId(), item);
    }

    public void addBidder(Bidder bidder) {
        bidders.put(bidder.getId(), bidder);
    }


    public boolean placeBid(String itemId, String bidderId, double amount) {

        Item item = items.get(itemId);
        Bidder bidder = bidders.get(bidderId);

        if (item == null || bidder == null) {
            System.out.println("Item or Bidder not found");
            return false;
        }

        double highest = getHighestBid(itemId);

        if (amount <= highest) {
            System.out.println("Bid must be higher than current: " + highest);
            return false;
        }

        if (!bidder.placeBid(amount)) {
            System.out.println("Not enough balance");
            return false;
        }

        BidTransaction bid = new BidTransaction(bidderId, amount);

        bids.putIfAbsent(itemId, new ArrayList<>());
        bids.get(itemId).add(bid);

        System.out.println("Bid success!");
        return true;
    }

    // =========================
    // GET HIGHEST BID
    // =========================

    public double getHighestBid(String itemId) {
        List<BidTransaction> list = bids.get(itemId);

        if (list == null || list.isEmpty()) return 0;

        double max = 0;
        for (BidTransaction b : list) {
            if (b.getAmount() > max) {
                max = b.getAmount();
            }
        }
        return max;
    }


    public String getWinner(String itemId) {
        List<BidTransaction> list = bids.get(itemId);

        if (list == null || list.isEmpty()) return null;

        BidTransaction best = list.get(0);

        for (BidTransaction b : list) {
            if (b.getAmount() > best.getAmount()) {
                best = b;
            }
        }

        return best.getBidderId();
    }

    // =========================
    // PRINT BID HISTORY
    // =========================

    public void printBids(String itemId) {
        List<BidTransaction> list = bids.get(itemId);

        if (list == null || list.isEmpty()) {
            System.out.println("No bids yet");
            return;
        }

        for (BidTransaction b : list) {
            System.out.println(
                    b.getBidderId() + " bid " + b.getAmount() + " at " + b.getTime()
            );
        }
    }


    public void printItems() {
        for (Item item : items.values()) {
            item.printInfo();
        }
    }
}