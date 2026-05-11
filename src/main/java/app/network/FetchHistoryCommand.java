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
import app.data.HistoryRequest;
import app.data.HistoryResponse;
import app.enums.PacketType;
import app.models.Auction;
import app.models.BidTransaction;
import app.models.Item;
import app.models.Packet;
import app.service.BidService;
import app.utils.JsonUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FetchHistoryCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, Packet packet) {
    HistoryRequest request = JsonUtil.fromJson(packet.getData(), HistoryRequest.class);
    int userId = request.userId();
    AuctionDAO auctionDAO = new MySqlAuctionDAO();
    BidDAO bidDAO = new MySqlBidDAO();
    AutoBidDAO autoBidDAO = new MySqlAutoBidDAO();
    ItemDAO itemDAO = new MySqlItemDAO();
    BidService bidService = new BidService(bidDAO, autoBidDAO, auctionDAO);
    List<AuctionSummary> summaries = new ArrayList<>();

    for (Auction auction : auctionDAO.findAll()) {
      boolean isSeller = auction.getSellerId() == userId;
      boolean hasBid = bidDAO.existsBySessionAndUser(auction.getId(), userId);
      if (!isSeller && !hasBid) {
        continue;
      }
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

    HistoryResponse response = new HistoryResponse(true, "OK", summaries);
    clientHandler.sendMessage(new Packet(PacketType.FETCH_HISTORY, JsonUtil.toJson(response)));
  }
}
