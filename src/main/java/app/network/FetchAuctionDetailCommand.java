package app.network;

import app.dao.AuctionDAO;
import app.dao.BidDAO;
import app.dao.ItemDAO;
import app.dao.impl.MySqlAuctionDAO;
import app.dao.impl.MySqlBidDAO;
import app.dao.impl.MySqlItemDAO;
import app.data.AuctionDetail;
import app.data.AuctionDetailRequest;
import app.data.AuctionDetailResponse;
import app.enums.PacketType;
import app.models.PacketReq;
import app.models.PacketRes;
import app.service.AuctionService;

public class FetchAuctionDetailCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    AuctionDetailRequest request = packet.getData(AuctionDetailRequest.class);
    AuctionDAO auctionDAO = new MySqlAuctionDAO();
    BidDAO bidDAO = new MySqlBidDAO();
    ItemDAO itemDAO = new MySqlItemDAO();
    AuctionService auctionService = new AuctionService(auctionDAO, bidDAO, itemDAO);

    try {
      AuctionDetail detail = auctionService.getAuctionDetail(request.auctionId());
      AuctionDetailResponse response = new AuctionDetailResponse(true, "OK", detail);
      clientHandler.sendMessage(PacketRes.of(PacketType.FETCH_AUCTION_DETAIL, response));
    } catch (app.exception.ServiceException e) {
      AuctionDetailResponse response =
          new AuctionDetailResponse(false, e.getMessage(), null);
      clientHandler.sendMessage(PacketRes.of(PacketType.FETCH_AUCTION_DETAIL, response));
    }
  }
}
