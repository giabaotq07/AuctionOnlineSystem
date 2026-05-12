package app.network;

import app.dao.AuctionDAO;
import app.dao.BidDAO;
import app.dao.ItemDAO;
import app.dao.impl.MySqlAuctionDAO;
import app.dao.impl.MySqlBidDAO;
import app.dao.impl.MySqlItemDAO;
import app.data.AuctionResultRequest;
import app.data.AuctionResultResponse;
import app.enums.PacketType;
import app.models.PacketReq;
import app.models.PacketRes;
import app.service.AuctionService;

public class FetchAuctionResultCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    AuctionResultRequest request = packet.getData(AuctionResultRequest.class);
    AuctionDAO auctionDAO = new MySqlAuctionDAO();
    BidDAO bidDAO = new MySqlBidDAO();
    ItemDAO itemDAO = new MySqlItemDAO();
    AuctionService auctionService = new AuctionService(auctionDAO, bidDAO, itemDAO);
    AuctionResultResponse response = auctionService.getAuctionResult(request.auctionId());
    clientHandler.sendMessage(PacketRes.of(PacketType.FETCH_AUCTION_RESULT, response));
  }
}
