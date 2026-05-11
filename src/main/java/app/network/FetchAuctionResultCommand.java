package app.network;

import app.dao.AuctionDAO;
import app.dao.AutoBidDAO;
import app.dao.BidDAO;
import app.dao.impl.MySqlAuctionDAO;
import app.dao.impl.MySqlAutoBidDAO;
import app.dao.impl.MySqlBidDAO;
import app.data.AuctionResultRequest;
import app.data.AuctionResultResponse;
import app.enums.PacketType;
import app.models.BidTransaction;
import app.models.PacketReq;
import app.models.PacketRes;
import app.service.AuctionService;
import app.service.BidService;
import java.util.Optional;

public class FetchAuctionResultCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    AuctionResultRequest request = packet.getData(AuctionResultRequest.class);
    AuctionDAO auctionDAO = new MySqlAuctionDAO();
    BidDAO bidDAO = new MySqlBidDAO();
    AutoBidDAO autoBidDAO = new MySqlAutoBidDAO();
    AuctionService auctionService = new AuctionService(auctionDAO, bidDAO);
    BidService bidService = new BidService(bidDAO, autoBidDAO, auctionDAO);

    auctionService.handleCompletion(request.auctionId());

    Optional<BidTransaction> highest = bidService.getHighestBid(request.auctionId());
    String winner = "chưa có người thắng";
    long price = 0;
    if (highest.isPresent()) {
      winner = highest.get().getBidderName();
      price = highest.get().getAmount();
    }

    AuctionResultResponse response = new AuctionResultResponse(true, "OK", winner, price);
    clientHandler.sendMessage(PacketRes.of(PacketType.FETCH_AUCTION_RESULT, response));
  }
}
