package app.network;

import app.dao.BidDAO;
import app.enums.CommandType;
import app.exceptions.ServiceException;
import app.models.Auction;
import app.models.BidTransaction;
import app.models.MessagePacket;
import java.util.List;

public class PlaceBidCommand implements Command {

  @Override
  public void execute(ClientHandler clientHandler, MessagePacket<?> packet) {
    try {
      Auction updatedAuction = (Auction) packet.getData();
      if (updatedAuction == null || updatedAuction.getBidHistory().isEmpty()) return;

      BidTransaction latestBid = updatedAuction.getBidHistory().get(updatedAuction.getBidHistory().size() - 1);

      // 1. Thực hiện lưu vào DB
      BidDAO.getInstance().placeBid(
              updatedAuction.getId(),
              latestBid.getBidder().getId(),
              latestBid.getAmount()
      );

      // 2. NẾU THÀNH CÔNG: Gửi cho tất cả mọi người
      Server.broadcast(new MessagePacket<>(CommandType.PLACE_BID, updatedAuction));
      System.out.println("[Server] Đặt giá thành công!");

    } catch (ServiceException e) {
      // ĐÂY LÀ CHỖ QUAN TRỌNG:
      // Khi giá bị thấp hơn trong DB, gửi trả về lỗi cho Client
      System.out.println("[Server] Từ chối Bid: " + e.getMessage());

      // Gửi một Packet loại ERROR kèm nội dung lỗi "Giá phải cao hơn..."
      clientHandler.sendMessage(new MessagePacket<>(CommandType.ERROR, e.getMessage()));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}