package app.service;

import app.dao.BidDao;
import app.dao.ItemDao;
import app.data.AuctionDetail;
import app.data.AuctionSummary;
import app.models.Auction;
import app.models.BidTransaction;
import app.models.Item;
import java.util.Optional;

/** AuctionMapper. */
public class AuctionMapper {
  private final ItemDao itemDao;
  private final BidDao bidDao;

  /** AuctionMapper. */
  public AuctionMapper(ItemDao itemDao, BidDao bidDao) {
    this.itemDao = itemDao;
    this.bidDao = bidDao;
  }

  /** toSummary. */
  public AuctionSummary toSummary(Auction auction) {
    Optional<Item> itemOpt = itemDao.findById(auction.getItemId());
    if (itemOpt.isEmpty()) {
      return null;
    }
    Item item = itemOpt.get();
    long currentPrice = getCurrentPrice(auction.getId(), item.getStartingPrice());
    return new AuctionSummary(auction, item.getName(), currentPrice);
  }

  /** toDetail. */
  public AuctionDetail toDetail(Auction auction) {
    Item item =
        itemDao
            .findById(auction.getItemId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy vật phẩm."));
    long currentPrice = getCurrentPrice(auction.getId(), item.getStartingPrice());
    return new AuctionDetail(
        auction.getId(),
        item.getName(),
        item.getDescription(),
        item.getStartingPrice(),
        item.getStepPrice(),
        currentPrice,
        auction.getEndTime(),
        auction.getVersion());
  }

  private long getCurrentPrice(int auctionId, long fallback) {
    return bidDao.findHighestBid(auctionId).map(BidTransaction::getAmount).orElse(fallback);
  }
}
