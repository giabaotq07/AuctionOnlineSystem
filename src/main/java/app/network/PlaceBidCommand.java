package app.network;

import app.data.AuctionSummary;
import app.data.AuctionsResponse;
import app.data.PlaceBidRequest;
import app.data.PlaceBidResponse;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.models.PacketReq;
import app.models.PacketRes;
import app.service.BidService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaceBidCommand implements Command {
  private Logger logger = LoggerFactory.getLogger(PlaceBidCommand.class);
  private final BidService bidService;

  public PlaceBidCommand(BidService bidService) {
    this.bidService = bidService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    PlaceBidRequest placeBidRequest = packet.getData(PlaceBidRequest.class);
    int sessionId = placeBidRequest.sessionId();
    long bidAmount = placeBidRequest.bidAmount();
    int bidderId = placeBidRequest.bidderId();
    try {
      PlaceBidResponse response =
          bidService.placeBidAndBuildResponse(sessionId, bidderId, bidAmount);
      PacketRes packetResponse = PacketRes.of(PacketType.PLACE_BID, response);
      clientHandler.sendMessage(packetResponse);
      Server.broadcast(packetResponse, clientHandler.getUser().getId());
    } catch (ServiceException e) {
      logger.error("Lỗi trả giá", "Giá đặt phải cao hơn giá hiện tại!");
    } catch (Exception e) {
      logger.error("Lỗi đặt giá", e.getMessage());
    }

    // tạm
    List<AuctionSummary> summaries = clientHandler.getAuctionService().getAuctionSummaries();
    AuctionsResponse auctionsResponse = new AuctionsResponse(true, "OK", summaries);
    Server.broadcast(PacketRes.of(PacketType.FETCH_AUCTIONS, auctionsResponse), -1);
  }
}
