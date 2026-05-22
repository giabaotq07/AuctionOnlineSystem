package app.client.manager;

import app.client.store.AuctionStore;
import app.common.dto.AuctionPreview;
import app.common.models.Auction;
import java.io.IOException;

/** Virtual proxy that lazy-loads full auction detail for the live screen. */
public final class AuctionDetailProxy {
  private final int auctionId;
  private boolean requestInFlight;

  public AuctionDetailProxy(int auctionId) {
    if (auctionId <= 0) {
      throw new IllegalArgumentException("Auction id must be positive.");
    }
    this.auctionId = auctionId;
  }

  public int getAuctionId() {
    return auctionId;
  }

  public AuctionPreview getPreview() {
    return AuctionStore.getInstance().getPreview(auctionId);
  }

  public Auction getDetailIfLoaded() {
    return AuctionStore.getInstance().getDetailIfLoaded(auctionId);
  }

  public boolean isDetailLoaded() {
    return AuctionStore.getInstance().hasDetail(auctionId);
  }

  public boolean isRequestInFlight() {
    return requestInFlight;
  }

  public boolean needsDetailRefresh() {
    Auction detail = getDetailIfLoaded();
    if (detail == null) {
      return true;
    }
    AuctionPreview preview = getPreview();
    return preview != null && preview.version() > detail.getVersion();
  }

  public void requestDetail() throws IOException {
    if (requestInFlight || !needsDetailRefresh()) {
      return;
    }
    requestInFlight = true;
    ClientRequestService.getInstance()
        .fetchAuctionDetail(auctionId, AuctionStore.getInstance().getKnownDetailVersion(auctionId));
  }

  public void finishRequest() {
    requestInFlight = false;
  }
}
