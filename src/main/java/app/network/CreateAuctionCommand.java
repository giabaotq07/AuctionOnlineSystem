package app.network;

import app.dao.AuctionDAO;
import app.dao.BidDAO;
import app.dao.ItemDAO;
import app.dao.impl.MySqlAuctionDAO;
import app.dao.impl.MySqlBidDAO;
import app.dao.impl.MySqlItemDAO;
import app.data.AuctionSummary;
import app.data.CreateAuctionRequest;
import app.data.CreateAuctionResponse;
import app.enums.PacketType;
import app.models.*;
import app.service.AuctionService;
import app.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateAuctionCommand implements Command {
  private static final Logger log = LoggerFactory.getLogger(CreateAuctionCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    CreateAuctionRequest request = packet.getData(CreateAuctionRequest.class);
    try {
      ItemDAO itemDAO = new MySqlItemDAO();
      ItemService itemService = new ItemService(itemDAO);
      Item item =
          ItemFactory.createItem(
              request.name(),
              request.sellerId(),
              request.description(),
              request.startingPrice(),
              request.stepPrice(),
              request.type());
      item = itemService.add(item);

      AuctionDAO auctionDAO = new MySqlAuctionDAO();
      BidDAO bidDAO = new MySqlBidDAO();
      AuctionService auctionService = new AuctionService(auctionDAO, bidDAO, itemDAO);
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
    new FetchAuctionsCommand().execute(clientHandler, null);
  }
}
