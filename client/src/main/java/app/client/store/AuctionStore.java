package app.client.store;

import app.common.enums.AuctionStatus;
import app.common.models.Auction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Stores full Auction models only. */
public final class AuctionStore {
  private static volatile AuctionStore instance;

  private final Map<Integer, Auction> auctionMap = new ConcurrentHashMap<>();

  private AuctionStore() {}

  /** getInstance. */
  public static AuctionStore getInstance() {
    if (instance == null) {
      synchronized (AuctionStore.class) {
        if (instance == null) {
          instance = new AuctionStore();
        }
      }
    }
    return instance;
  }

  /** addAuction. */
  public void addAuction(Auction auction) {
    if (auction == null) {
      return;
    }
    auctionMap.merge(auction.getId(), auction, this::mergeAuction);
  }

  /** getAuction. */
  public Auction getAuction(int auctionId) {
    return auctionMap.get(auctionId);
  }

  /** getAuctions. */
  public List<Auction> getAuctions() {
    return new ArrayList<>(auctionMap.values());
  }

  /** updateBid. */
  public void updateBid(long auctionId, long highestBid, long bidderId) {
    Auction auction =
        auctionMap.computeIfAbsent(toIntId(auctionId), id -> partialAuction(id, highestBid));
    auction.setHighestBid(highestBid);
    if (bidderId <= Integer.MAX_VALUE) {
      auction.setWinnerId((int) bidderId);
    }
  }

  /** markCanceled. */
  public void markCanceled(int auctionId) {
    Auction auction = auctionMap.computeIfAbsent(auctionId, id -> partialAuction(id, 0));
    auction.setStatus(AuctionStatus.CANCELED);
  }

  /** markFinished. */
  public void markFinished(long auctionId, long finalPrice) {
    Auction auction =
        auctionMap.computeIfAbsent(toIntId(auctionId), id -> partialAuction(id, finalPrice));
    auction.setStatus(AuctionStatus.FINISHED);
    auction.setHighestBid(finalPrice);
  }

  private Auction mergeAuction(Auction existing, Auction incoming) {
    boolean incomingHasDetail = incoming.getItemId() > 0 || incoming.getSellerId() > 0;
    Auction merged =
        new Auction(
            existing.getId(),
            incoming.getItemId() > 0 ? incoming.getItemId() : existing.getItemId(),
            incoming.getSellerId() > 0 ? incoming.getSellerId() : existing.getSellerId(),
            incoming.getWinnerId() != null ? incoming.getWinnerId() : existing.getWinnerId(),
            incoming.getStatus() != null ? incoming.getStatus() : existing.getStatus(),
            incoming.getStartTime() != null ? incoming.getStartTime() : existing.getStartTime(),
            incoming.getEndTime() != null ? incoming.getEndTime() : existing.getEndTime(),
            incoming.getHighestBid() > 0 ? incoming.getHighestBid() : existing.getHighestBid(),
            incomingHasDetail ? incoming.getExtendedCount() : existing.getExtendedCount(),
            Math.max(existing.getVersion(), incoming.getVersion()),
            incoming.getCreatedAt() != null ? incoming.getCreatedAt() : existing.getCreatedAt(),
            incoming.getUpdatedAt() != null ? incoming.getUpdatedAt() : existing.getUpdatedAt());
    merged.setItemName(
        incoming.getItemName() != null ? incoming.getItemName() : existing.getItemName());
    return merged;
  }

  private Auction partialAuction(int auctionId, long highestBid) {
    return new Auction(auctionId, 0, 0, null, null, null, null, highestBid, 0, 0, null, null);
  }

  private int toIntId(long id) {
    if (id > Integer.MAX_VALUE || id < Integer.MIN_VALUE) {
      throw new IllegalArgumentException("Id is out of int range: " + id);
    }
    return (int) id;
  }
}
