package app.network;

import app.data.AuctionSummary;
import app.data.AuctionsResponse;
import app.data.CreateAuctionRequest;
import app.data.CreateAuctionResponse;
import app.enums.PacketType;
import app.models.*;
import app.service.AuctionService;
import app.service.ItemService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateAuctionCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(CreateAuctionCommand.class);
  private final AuctionService auctionService;
  private final ItemService itemService;

  public CreateAuctionCommand(AuctionService auctionService, ItemService itemService) {
    this.auctionService = auctionService;
    this.itemService = itemService;
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
        clientHandler.sendPacket(
            PacketRes.of(
                true,
                PacketType.CREATE_AUCTION,
                new CreateAuctionResponse(false, "Invalid request", null)));
        return;
      }
      User user = clientHandler.getUser();
      // KHÔNG trust sellerId từ client
      Item item =
          ItemFactory.createItem(
              request.name(),
              user.getId(),
              request.description(),
              request.startingPrice(),
              request.stepPrice(),
              request.type());
      item = itemService.add(item);
      Auction auction =
          auctionService.createAndStartAuction(
              item.getId(), user.getId(), item.getStartingPrice(), request.durationMinutes());
      AuctionSummary summary = new AuctionSummary(auction, item.getName(), auction.getHighestBid());
      CreateAuctionResponse response =
          new CreateAuctionResponse(true, "Tạo phiên thành công", summary);
      PacketRes packetRes = PacketRes.of(true, PacketType.CREATE_AUCTION, response);
      // response cho creator
      clientHandler.sendPacket(packetRes);
      // broadcast auction mới
      Server.broadcast(packetRes, user.getId());
      // broadcast refresh list
      broadcastAuctionList();
      logger.info("Auction created successfully by user {}", user.getId());
    } catch (Exception e) {
      logger.error("Create auction failed", e);
      clientHandler.sendPacket(
          PacketRes.of(false, PacketType.CREATE_AUCTION, "Tạo phiên thất bại"));
    }
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
