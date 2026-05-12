package app.service;

import app.dao.BidDAO;
import app.dao.ItemDAO;
import app.data.AuctionDetail;
import app.data.AuctionSummary;
import app.models.Auction;
import app.models.BidTransaction;
import app.models.Item;
import java.util.Optional;

public class AuctionMapper {
  private final ItemDAO itemDAO;
  private final BidDAO bidDAO;

  public AuctionMapper(ItemDAO itemDAO, BidDAO bidDAO) {
    this.itemDAO = itemDAO;
    this.bidDAO = bidDAO;
  }

  public AuctionSummary toSummary(Auction auction) {
    Optional<Item> itemOpt = itemDAO.findById(auction.getItemId());
    if (itemOpt.isEmpty()) {
      return null;
    }
    Item item = itemOpt.get();
    long currentPrice = getCurrentPrice(auction.getId(), item.getStartingPrice());
    return new AuctionSummary(auction, item.getName(), currentPrice);
  }

  public AuctionDetail toDetail(Auction auction) {
    Item item =
        itemDAO
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
        auction.getEndTime());
  }

  private long getCurrentPrice(int auctionId, long fallback) {
    return bidDAO.findHighestBid(auctionId).map(BidTransaction::getAmount).orElse(fallback);
  }
}
