package app.network;

import app.dao.AuctionDAO;
import app.dao.AutoBidDAO;
import app.dao.BidDAO;
import app.dao.ItemDAO;
import app.dao.impl.MySqlAuctionDAO;
import app.dao.impl.MySqlAutoBidDAO;
import app.dao.impl.MySqlBidDAO;
import app.dao.impl.MySqlItemDAO;
import app.data.AuctionDetail;
import app.data.AuctionDetailRequest;
import app.data.AuctionDetailResponse;
import app.enums.PacketType;
import app.models.Auction;
import app.models.BidTransaction;
import app.models.Item;
import app.models.Packet;
import app.service.BidObserverService;
import app.service.BidService;
import app.utils.JsonUtil;
import java.util.Optional;

public class FetchAuctionDetailCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, Packet packet) {
    AuctionDetailRequest request = JsonUtil.fromJson(packet.getData(), AuctionDetailRequest.class);
    AuctionDAO auctionDAO = new MySqlAuctionDAO();
    BidDAO bidDAO = new MySqlBidDAO();
    AutoBidDAO autoBidDAO = new MySqlAutoBidDAO();
    ItemDAO itemDAO = new MySqlItemDAO();
    BidService bidService =
        new BidService(bidDAO, autoBidDAO, auctionDAO, new BidObserverService());

    Optional<Auction> auctionOpt = auctionDAO.findById(request.auctionId());
    if (auctionOpt.isEmpty()) {
      AuctionDetailResponse response =
          new AuctionDetailResponse(false, "Không tìm thấy phiên", null);
      clientHandler.sendMessage(
          new Packet(PacketType.FETCH_AUCTION_DETAIL, JsonUtil.toJson(response)));
      return;
    }

    Auction auction = auctionOpt.get();
    Optional<Item> itemOpt = itemDAO.findById(auction.getItemId());
    if (itemOpt.isEmpty()) {
      AuctionDetailResponse response =
          new AuctionDetailResponse(false, "Không tìm thấy vật phẩm", null);
      clientHandler.sendMessage(
          new Packet(PacketType.FETCH_AUCTION_DETAIL, JsonUtil.toJson(response)));
      return;
    }

    Item item = itemOpt.get();
    long currentPrice = item.getStartingPrice();
    Optional<BidTransaction> highest = bidService.getHighestBid(auction.getId());
    if (highest.isPresent()) {
      currentPrice = highest.get().getAmount();
    }

    AuctionDetail detail =
        new AuctionDetail(
            auction.getId(),
            item.getName(),
            item.getDescription(),
            item.getStartingPrice(),
            item.getStepPrice(),
            currentPrice,
            auction.getEndTime());

    AuctionDetailResponse response = new AuctionDetailResponse(true, "OK", detail);
    clientHandler.sendMessage(
        new Packet(PacketType.FETCH_AUCTION_DETAIL, JsonUtil.toJson(response)));
  }
}
