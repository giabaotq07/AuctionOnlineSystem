package app.network;

import app.dto.AuctionSummariesResponse;
import app.dto.AuctionSummary;
import app.dto.CreateAuctionRequest;
import app.dto.CreateAuctionResponse;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.mapper.DtoMapper;
import app.models.Auction;
import app.models.Item;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.User;
import app.service.AuctionService;
import app.service.ItemService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CreateAuctionCommand. */
public class CreateAuctionCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(CreateAuctionCommand.class);
  private final AuctionService auctionService;
  private final ItemService itemService;

  /** CreateAuctionCommand. */
  public CreateAuctionCommand(AuctionService auctionService, ItemService itemService) {
    this.auctionService = auctionService;
    this.itemService = itemService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      if (!clientHandler.isAuthenticated()) {
        clientHandler.sendPacket(
            PacketRes.error(PacketType.CREATE_AUCTION, "Authentication required"));
        return;
      }
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
              user.getRole());
      AuctionSummary summary = toSummary(auction);
      CreateAuctionResponse response = new CreateAuctionResponse(summary);
      PacketRes packetRes =
          PacketRes.of(true, PacketType.CREATE_AUCTION, "Tạo phiên thành công", response);
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
    clientHandler.sendPacket(PacketRes.error(PacketType.CREATE_AUCTION, message));
  }

  private void broadcastAuctionList() {
    try {
      List<AuctionSummary> summaries = buildAuctionSummaries();
      AuctionSummariesResponse response = new AuctionSummariesResponse(summaries);
      Server.broadcast(PacketRes.of(true, PacketType.FETCH_AUCTION_SUMMARIES, response), -1);
    } catch (Exception e) {
      logger.error("Failed to broadcast auction list", e);
    }
  }

  private List<AuctionSummary> buildAuctionSummaries() {
    return auctionService.getAllAuctions().stream().map(this::toSummary).toList();
  }

  private AuctionSummary toSummary(Auction auction) {
    Item item =
        itemService
            .getById(auction.getItemId())
            .orElseThrow(() -> new ServiceException("Không tìm thấy vật phẩm."));
    return DtoMapper.toAuctionSummary(auction, item);
  }
}
