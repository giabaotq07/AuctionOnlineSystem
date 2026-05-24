package app.common.dto;

/** AuctionDetailResponse. */
public record AuctionDetailResponse(
    AuctionDto auction, int auctionId, int version, boolean notModified) implements Response {
  public AuctionDetailResponse(AuctionDto auction) {
    this(
        auction,
        auction == null ? 0 : auction.id(),
        auction == null ? -1 : auction.version(),
        false);
  }

  public static AuctionDetailResponse notModified(int auctionId, int version) {
    return new AuctionDetailResponse(null, auctionId, version, true);
  }
}
