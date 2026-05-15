package app.network;

import app.data.AuctionSummary;
import app.data.AuctionsResponse;
import app.data.CreateAuctionRequest;
import app.data.CreateAuctionResponse;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.User;
import app.service.AuctionService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CreateAuctionCommand. */
public class CreateAuctionCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(CreateAuctionCommand.class);
  private final AuctionService auctionService;

  /** CreateAuctionCommand. */
  public CreateAuctionCommand(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      if (!clientHandler.isAuthenticated()) {
        clientHandler.sendPacket(PacketRes.error("Authentication required"));
        return;
      }
      CreateAuctionRequest request = packet.getData(CreateAuctionRequest.class);
      if (request == null) {
        sendError(clientHandler, "Invalid request");
        return;
      }
      User user = clientHandler.getUser();
      AuctionSummary summary =
          auctionService.createAndStartAuctionWithItem(
              request.name(),
              request.description(),
              request.startingPrice(),
              request.stepPrice(),
              request.type(),
              request.durationMinutes(),
              user.getId(),
              user.getRole());
      CreateAuctionResponse response =
          new CreateAuctionResponse(true, "Tạo phiên thành công", summary);
      PacketRes packetRes = PacketRes.of(true, PacketType.CREATE_AUCTION, response);
      clientHandler.sendPacket(packetRes);
      Server.broadcast(packetRes, user.getId());
      broadcastAuctionList();
      logger.info("Auction created successfully by user {}", user.getId());
    } catch (ServiceException e) {
      logger.warn("Create auction failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Create auction failed", e);
      sendError(clientHandler, "Tạo phiên thất bại");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(
        PacketRes.of(
            false, PacketType.CREATE_AUCTION, new CreateAuctionResponse(false, message, null)));
  }

  private void broadcastAuctionList() {
    try {
      List<AuctionSummary> summaries = auctionService.getAuctionSummaries();
      AuctionsResponse response = new AuctionsResponse(true, "OK", summaries);
      Server.broadcast(PacketRes.of(true, PacketType.FETCH_AUCTIONS, response), -1);
    } catch (Exception e) {
      logger.error("Failed to broadcast auction list", e);
    }
  }
}
