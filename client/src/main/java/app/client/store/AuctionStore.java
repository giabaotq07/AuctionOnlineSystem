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
    auctionMap.put(auction.getId(), auction);
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
    Auction auction = getAuction(toIntId(auctionId));
    if (auction == null) {
      return;
    }
    auction.setHighestBid(highestBid);
    if (bidderId <= Integer.MAX_VALUE) {
      auction.setWinnerId((int) bidderId);
    }
  }

  /** markCanceled. */
  public void markCanceled(int auctionId) {
    Auction auction = getAuction(auctionId);
    if (auction != null) {
      auction.setStatus(AuctionStatus.CANCELED);
    }
  }

  /** markFinished. */
  public void markFinished(long auctionId, long finalPrice) {
    Auction auction = getAuction(toIntId(auctionId));
    if (auction == null) {
      return;
    }
    auction.setStatus(AuctionStatus.FINISHED);
    auction.setHighestBid(finalPrice);
  }

  private int toIntId(long id) {
    if (id > Integer.MAX_VALUE || id < Integer.MIN_VALUE) {
      throw new IllegalArgumentException("Id is out of int range: " + id);
    }
    return (int) id;
  }
}
