package app.network;

import app.dao.AuctionDAO;
import app.dao.BidDAO;
import app.dao.ItemDAO;
import app.dao.impl.MySqlAuctionDAO;
import app.dao.impl.MySqlBidDAO;
import app.dao.impl.MySqlItemDAO;
import app.data.CreateAuctionRequest;
import app.data.CreateAuctionResponse;
import app.enums.PacketType;
import app.models.Auction;
import app.models.Item;
import app.models.ItemFactory;
import app.models.Packet;
import app.service.AuctionService;
import app.service.ItemService;
import app.utils.JsonUtil;
import java.time.LocalDateTime;

public class CreateAuctionCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, Packet packet) {
    CreateAuctionRequest request = JsonUtil.fromJson(packet.getData(), CreateAuctionRequest.class);
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
      AuctionService auctionService = new AuctionService(auctionDAO, bidDAO);
      Auction session =
          new Auction(
              item.getId(),
              request.sellerId(),
              LocalDateTime.now().plusMinutes(request.durationMinutes()));
      session.start();
      session = auctionService.createAuction(session);

      CreateAuctionResponse response =
          new CreateAuctionResponse(true, "Tạo phiên thành công", session);
      clientHandler.sendMessage(new Packet(PacketType.CREATE_AUCTION, JsonUtil.toJson(response)));
    } catch (Exception e) {
      CreateAuctionResponse response =
          new CreateAuctionResponse(false, "Tạo phiên thất bại: " + e.getMessage(), null);
      clientHandler.sendMessage(new Packet(PacketType.CREATE_AUCTION, JsonUtil.toJson(response)));
    }
  }
}
