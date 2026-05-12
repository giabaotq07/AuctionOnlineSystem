package app.network;

import app.dao.AuctionDAO;
import app.dao.AutoBidDAO;
import app.dao.BidDAO;
import app.dao.impl.MySqlAuctionDAO;
import app.dao.impl.MySqlAutoBidDAO;
import app.dao.impl.MySqlBidDAO;
import app.dao.impl.MySqlItemDAO;
import app.data.PlaceBidRequest;
import app.data.PlaceBidResponse;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.models.BidTransaction;
import app.models.PacketReq;
import app.models.PacketRes;
import app.service.BidService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaceBidCommand implements Command {
  private Logger logger = LoggerFactory.getLogger(PlaceBidCommand.class);
  private final BidService bidService;

  public PlaceBidCommand() {
    AuctionDAO auctionDAO = new MySqlAuctionDAO();
    BidDAO bidDAO = new MySqlBidDAO();
    AutoBidDAO autoBidDAO = new MySqlAutoBidDAO();

    this.bidService = new BidService(bidDAO, autoBidDAO, auctionDAO, new MySqlItemDAO());
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    PlaceBidRequest placeBidRequest = packet.getData(PlaceBidRequest.class);
    int sessionId = placeBidRequest.sessionId();
    long bidAmount = placeBidRequest.bidAmount();
    int bidderId = placeBidRequest.bidderId();
    try {
      PlaceBidResponse response = bidService.placeBidAndBuildResponse(sessionId, bidderId, bidAmount);
      PacketRes packetResponse = PacketRes.of(PacketType.PLACE_BID, response);
      clientHandler.sendMessage(packetResponse);
      Server.broadcast(packetResponse, clientHandler.getUser().getId());
    } catch (ServiceException e) {
      logger.error("Lỗi trả giá", "Giá đặt phải cao hơn giá hiện tại!");
    } catch (Exception e) {
      logger.error("Lỗi đặt giá", e.getMessage());
    }
    new FetchAuctionsCommand().execute(clientHandler, null);
  }
}
