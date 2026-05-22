package app.client.manager;

import app.client.store.AuctionStore;
import app.common.dto.AuctionPreview;

/** Session-scoped state for the currently opened live auction screen. */
public final class LiveAuctionSessionStore {
  private static volatile LiveAuctionSessionStore instance;

  private Integer selectedAuctionId;
  private AuctionDetailProxy selectedProxy;

  private LiveAuctionSessionStore() {}

  /** getInstance. */
  public static LiveAuctionSessionStore getInstance() {
    if (instance == null) {
      synchronized (LiveAuctionSessionStore.class) {
        if (instance == null) {
          instance = new LiveAuctionSessionStore();
        }
      }
    }
    return instance;
  }

  public synchronized void selectAuction(AuctionPreview preview) {
    if (preview == null || preview.auctionId() <= 0) {
      return;
    }
    AuctionStore.getInstance().addPreview(preview);
    selectAuction(preview.auctionId());
  }

  public synchronized void selectAuction(int auctionId) {
    if (auctionId <= 0) {
      return;
    }
    selectedAuctionId = auctionId;
    selectedProxy = new AuctionDetailProxy(auctionId);
  }

  public synchronized Integer getSelectedAuctionId() {
    return selectedAuctionId;
  }

  public synchronized AuctionDetailProxy getSelectedProxy() {
    if (selectedProxy == null && selectedAuctionId != null) {
      selectedProxy = new AuctionDetailProxy(selectedAuctionId);
    }
    return selectedProxy;
  }

  public synchronized void finishDetailRequest(int auctionId) {
    if (selectedProxy != null && selectedProxy.getAuctionId() == auctionId) {
      selectedProxy.finishRequest();
    }
  }

  public synchronized void clear() {
    selectedAuctionId = null;
    selectedProxy = null;
  }
}
