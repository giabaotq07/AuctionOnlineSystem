package app.client.store;

import app.common.dto.AuctionPreview;
import app.common.enums.AuctionStatus;
import app.common.models.Auction;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Stores lightweight auction previews separately from full detail aggregates. */
public final class AuctionStore {
  private static volatile AuctionStore instance;

  private final Map<Integer, AuctionPreview> previewMap = new ConcurrentHashMap<>();
  private final Map<Integer, Auction> detailMap = new ConcurrentHashMap<>();
  private final Object historyLock = new Object();
  private List<Integer> historyAuctionIds = new ArrayList<>();

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

  public void addPreview(AuctionPreview preview) {
    if (preview == null || preview.auctionId() <= 0) {
      return;
    }
    previewMap.merge(preview.auctionId(), preview, this::mergePreview);
  }

  public void addDetail(Auction auction) {
    if (auction == null || auction.getId() <= 0) {
      return;
    }
    if (auction.getItem() != null) {
      ItemStore.getInstance().addItem(auction.getItem());
    }
    detailMap.merge(auction.getId(), auction, this::mergeDetail);
    addPreview(AuctionPreview.from(auction));
  }

  public void addAuction(Auction auction) {
    addDetail(auction);
  }

  public AuctionPreview getPreview(int auctionId) {
    return previewMap.get(auctionId);
  }

  public List<AuctionPreview> getAuctionPreviews() {
    return new ArrayList<>(previewMap.values());
  }

  public Auction getDetailIfLoaded(int auctionId) {
    return detailMap.get(auctionId);
  }

  public boolean hasDetail(int auctionId) {
    return detailMap.containsKey(auctionId);
  }

  public int getKnownDetailVersion(int auctionId) {
    Auction detail = detailMap.get(auctionId);
    return detail == null ? -1 : detail.getVersion();
  }

  public Auction getAuction(int auctionId) {
    return getDetailIfLoaded(auctionId);
  }

  public void setHistoryAuctions(List<AuctionPreview> auctions) {
    Set<Integer> nextIds = new LinkedHashSet<>();
    if (auctions != null) {
      for (AuctionPreview preview : auctions) {
        if (preview == null || preview.auctionId() <= 0) {
          continue;
        }
        nextIds.add(preview.auctionId());
        addPreview(preview);
      }
    }
    synchronized (historyLock) {
      historyAuctionIds = new ArrayList<>(nextIds);
    }
  }

  public List<AuctionPreview> getHistoryAuctionPreviews() {
    List<Integer> ids;
    synchronized (historyLock) {
      ids = new ArrayList<>(historyAuctionIds);
    }
    List<AuctionPreview> auctions = new ArrayList<>();
    for (Integer id : ids) {
      AuctionPreview preview = id == null ? null : previewMap.get(id);
      if (preview != null) {
        auctions.add(preview);
      }
    }
    return auctions;
  }

  public void appendHistoryAuctions(List<AuctionPreview> auctions) {
    if (auctions == null || auctions.isEmpty()) {
      return;
    }
    synchronized (historyLock) {
      Set<Integer> merged = new LinkedHashSet<>(historyAuctionIds);
      for (AuctionPreview preview : auctions) {
        if (preview == null || preview.auctionId() <= 0) {
          continue;
        }
        merged.add(preview.auctionId());
        addPreview(preview);
      }
      historyAuctionIds = new ArrayList<>(merged);
    }
  }

  public int getMaxHistoryVersion() {
    int maxVersion = -1;
    for (AuctionPreview preview : getHistoryAuctionPreviews()) {
      if (preview != null && preview.version() > maxVersion) {
        maxVersion = preview.version();
      }
    }
    return maxVersion;
  }

  public void clearHistory() {
    synchronized (historyLock) {
      historyAuctionIds = new ArrayList<>();
    }
  }

  public void updateBid(long auctionId, long highestBid, long bidderId) {
    int id = toIntId(auctionId);
    previewMap.computeIfPresent(id, (ignored, preview) -> preview.withHighestBid(highestBid));
    Auction detail = detailMap.get(id);
    if (detail != null) {
      detail.setHighestBid(highestBid);
      if (bidderId > 0 && bidderId <= Integer.MAX_VALUE) {
        detail.setWinnerId((int) bidderId);
      }
    }
  }

  public void markCanceled(int auctionId) {
    previewMap.computeIfPresent(
        auctionId, (ignored, preview) -> preview.withStatus(AuctionStatus.CANCELED));
    Auction detail = detailMap.get(auctionId);
    if (detail != null) {
      detail.setStatus(AuctionStatus.CANCELED);
    }
  }

  public void markFinished(long auctionId, long finalPrice) {
    markFinished(auctionId, finalPrice, null);
  }

  public void markFinished(long auctionId, long finalPrice, Integer winnerId) {
    int id = toIntId(auctionId);
    previewMap.computeIfPresent(
        id,
        (ignored, preview) -> preview.withStatusAndHighestBid(AuctionStatus.FINISHED, finalPrice));
    Auction detail = detailMap.get(id);
    if (detail != null) {
      detail.setStatus(AuctionStatus.FINISHED);
      detail.setHighestBid(finalPrice);
      if (winnerId != null && winnerId > 0) {
        detail.setWinnerId(winnerId);
      }
    }
  }

  private AuctionPreview mergePreview(AuctionPreview existing, AuctionPreview incoming) {
    if (incoming.version() < existing.version()) {
      return existing;
    }
    return incoming;
  }

  private Auction mergeDetail(Auction existing, Auction incoming) {
    if (incoming.getVersion() < existing.getVersion()) {
      return existing;
    }
    if (incoming.getItem() == null) {
      incoming.setItem(existing.getItem());
    }
    if (incoming.getSeller() == null) {
      incoming.setSeller(existing.getSeller());
    }
    if (incoming.getWinner() == null) {
      incoming.setWinner(existing.getWinner());
    }
    if (incoming.getBids().isEmpty()) {
      incoming.setBids(existing.getBids());
    }
    return incoming;
  }

  private int toIntId(long id) {
    if (id > Integer.MAX_VALUE || id < Integer.MIN_VALUE) {
      throw new IllegalArgumentException("Id is out of int range: " + id);
    }
    return (int) id;
  }
}
