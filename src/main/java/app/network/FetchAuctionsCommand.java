package app.network;

import app.dao.AuctionDAO;
import app.dao.AutoBidDAO;
import app.dao.BidDAO;
import app.dao.ItemDAO;
import app.dao.impl.MySqlAuctionDAO;
import app.dao.impl.MySqlAutoBidDAO;
import app.dao.impl.MySqlBidDAO;
import app.dao.impl.MySqlItemDAO;
import app.data.AuctionSummary;
import app.data.AuctionsResponse;
import app.enums.PacketType;
import app.models.Auction;
import app.models.BidTransaction;
import app.models.Item;
import app.models.Packet;
import app.service.BidObserverService;
import app.service.BidService;
import app.utils.JsonUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FetchAuctionsCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, Packet packet) {
    AuctionDAO auctionDAO = new MySqlAuctionDAO();
    BidDAO bidDAO = new MySqlBidDAO();
    AutoBidDAO autoBidDAO = new MySqlAutoBidDAO();
    ItemDAO itemDAO = new MySqlItemDAO();
    BidService bidService =
        new BidService(bidDAO, autoBidDAO, auctionDAO, new BidObserverService());
    List<AuctionSummary> summaries = new ArrayList<>();

    for (Auction auction : auctionDAO.findAll()) {
      Optional<Item> itemOpt = itemDAO.findById(auction.getItemId());
      if (itemOpt.isEmpty()) {
        continue;
      }
      Item item = itemOpt.get();
      long currentPrice = item.getStartingPrice();
      Optional<BidTransaction> highest = bidService.getHighestBid(auction.getId());
      if (highest.isPresent()) {
        currentPrice = highest.get().getAmount();
      }
      summaries.add(new AuctionSummary(auction, item.getName(), currentPrice));
    }

    AuctionsResponse response = new AuctionsResponse(true, "OK", summaries);
    clientHandler.sendMessage(new Packet(PacketType.FETCH_AUCTIONS, JsonUtil.toJson(response)));
  }
}
