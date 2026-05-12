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
  private static final Logger log = LoggerFactory.getLogger(CreateAuctionCommand.class);

  private final AuctionService auctionService;
  private final ItemService itemService;

  public CreateAuctionCommand(AuctionService auctionService, ItemService itemService) {
    this.auctionService = auctionService;
    this.itemService = itemService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    CreateAuctionRequest request = packet.getData(CreateAuctionRequest.class);
    try {
      Item item =
          ItemFactory.createItem(
              request.name(),
              request.sellerId(),
              request.description(),
              request.startingPrice(),
              request.stepPrice(),
              request.type());
      item = itemService.add(item);

      Auction session =
          auctionService.createAndStartAuction(
              item.getId(), request.sellerId(), item.getStartingPrice(), request.durationMinutes());
      AuctionSummary auctionSummary =
          new AuctionSummary(session, item.getName(), session.getHighestBid());
      CreateAuctionResponse response =
          new CreateAuctionResponse(true, "Tạo phiên thành công", auctionSummary);
      PacketRes packetRes = PacketRes.of(PacketType.CREATE_AUCTION, response);
      clientHandler.sendMessage(packetRes);
      Server.broadcast(packetRes, clientHandler.getUser().getId());
    } catch (Exception e) {

      e.printStackTrace();
      CreateAuctionResponse response =
          new CreateAuctionResponse(false, "Tạo phiên thất bại: " + e.getMessage(), null);
      clientHandler.sendMessage(PacketRes.of(PacketType.CREATE_AUCTION, response));
    }
    // tạm
    List<AuctionSummary> summaries = clientHandler.getAuctionService().getAuctionSummaries();
    AuctionsResponse auctionsResponse = new AuctionsResponse(true, "OK", summaries);
    Server.broadcast(PacketRes.of(PacketType.FETCH_AUCTIONS, auctionsResponse), -1);
  }
}
