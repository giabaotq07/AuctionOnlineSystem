package app.network;

import app.data.AuctionSummary;
import app.data.AuctionsResponse;
import app.data.PlaceBidRequest;
import app.data.PlaceBidResponse;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.User;
import app.service.BidService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaceBidCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(PlaceBidCommand.class);
  private final BidService bidService;

  public PlaceBidCommand(BidService bidService) {
    this.bidService = bidService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      if (!clientHandler.isAuthenticated()) {
        sendError(clientHandler, "Authentication required");
        return;
      }
      PlaceBidRequest request = packet.getData(PlaceBidRequest.class);
      if (request == null) {
        sendError(clientHandler, "Dữ liệu đặt giá không hợp lệ.");
        return;
      }
      int sessionId = request.sessionId();
      long bidAmount = request.bidAmount();
      if (sessionId <= 0) {
        sendError(clientHandler, "Phiên đấu giá không hợp lệ.");
        return;
      }
      if (bidAmount <= 0) {
        sendError(clientHandler, "Giá đặt không hợp lệ.");
        return;
      }
      User user = clientHandler.getUser();
      // KHÔNG trust bidderId từ client
      int bidderId = user.getId();
      PlaceBidResponse response =
          bidService.placeBidAndBuildResponse(sessionId, bidderId, bidAmount);
      PacketRes packetResponse = PacketRes.of(PacketType.PLACE_BID, response);
      // sender
      clientHandler.sendPacket(packetResponse);
      // others
      Server.broadcast(packetResponse, bidderId);
      // refresh auction list
      broadcastAuctionList(clientHandler);
      logger.info("User {} placed bid {} in auction {}", bidderId, bidAmount, sessionId);
    } catch (ServiceException e) {
      logger.warn("Place bid failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected place bid error", e);
      sendError(clientHandler, "Không thể đặt giá.");
    }
  }

  private void broadcastAuctionList(ClientHandler clientHandler) {
    try {
      List<AuctionSummary> summaries = clientHandler.getAuctionService().getAuctionSummaries();
      AuctionsResponse response = new AuctionsResponse(true, "OK", summaries);
      Server.broadcast(PacketRes.of(PacketType.FETCH_AUCTIONS, response), -1);
    } catch (Exception e) {
      logger.error("Failed to broadcast auction list", e);
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(PacketType.PLACE_BID, message));
  }
}
