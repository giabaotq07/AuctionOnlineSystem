package app.server.command;

import app.common.dto.*;
import app.common.enums.ResponseType;
import app.common.exception.ServiceException;
import app.common.models.*;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.AuctionService;
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
      CreateAuctionRequest request = packet.getData(CreateAuctionRequest.class);
      if (request == null) {
        sendError(clientHandler, "Invalid request");
        return;
      }
      User user = clientHandler.getUser();
      Auction auction =
          auctionService.createAndStartAuctionWithItem(
              request.name(),
              request.description(),
              request.startingPrice(),
              request.stepPrice(),
              request.type(),
              request.durationMinutes(),
              user.getId(),
              user.getRole(),
              request.startTime());
      CreateAuctionResponse response =
          new CreateAuctionResponse(auctionService.getAuctionDetail(auction.getId()));
      PacketRes packetRes =
          PacketRes.of(ResponseType.CREATE_AUCTION_RESULT, "Tạo phiên thành công", response);
      clientHandler.sendPacket(packetRes);
      Server.broadcast(
          PacketRes.of(ResponseType.AUCTION_CREATED, "Có phiên đấu giá mới.", response),
          user.getId());
      Server.broadcastAuctionList(auctionService);

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
    clientHandler.sendPacket(PacketRes.error(ResponseType.ERROR, message));
  }
}
