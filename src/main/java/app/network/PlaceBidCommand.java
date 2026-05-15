package app.network;

import app.dto.AuctionSummary;
import app.dto.AuctionsResponse;
import app.dto.PlaceBidRequest;
import app.dto.PlaceBidResponse;
import app.dto.UserData;
import app.dto.WalletUpdateResponse;
import app.enums.PacketType;
import app.enums.UserRole;
import app.exception.ServiceException;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.User;
import app.service.AuctionService;
import app.service.BidService;
import app.service.UserService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** PlaceBidCommand. */
public class PlaceBidCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(PlaceBidCommand.class);
  private final BidService bidService;
  private final UserService userService;
  private final AuctionService auctionService;

  /** PlaceBidCommand. */
  public PlaceBidCommand(
      BidService bidService, UserService userService, AuctionService auctionService) {
    this.bidService = bidService;
    this.userService = userService;
    this.auctionService = auctionService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    int auctionId = 0;
    int bidderId = 0;
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
      auctionId = request.auctionId();
      long bidAmount = request.bidAmount();
      if (auctionId <= 0) {
        sendError(clientHandler, "Phiên đấu giá không hợp lệ.");
        return;
      }
      if (bidAmount <= 0) {
        sendError(clientHandler, "Giá đặt không hợp lệ.");
        return;
      }
      User user = clientHandler.getUser();
      if (user.getRole() != UserRole.BIDDER) {
        sendError(clientHandler, "Chỉ Bidder được đặt giá.");
        return;
      }
      bidderId = user.getId();
      PlaceBidResponse response = bidService.placeBid(auctionId, bidderId, bidAmount);
      auctionService.invalidateCache();
      PacketRes packetResponse =
          PacketRes.of(true, PacketType.PLACE_BID, "Đặt giá thành công.", response);
      clientHandler.sendPacket(packetResponse);
      Server.broadcast(packetResponse, bidderId);
      sendWalletUpdate(clientHandler, userService.getById(bidderId));
      broadcastAuctionList(clientHandler);
      logger.info("User {} placed bid {} in auction {}", bidderId, bidAmount, auctionId);
    } catch (ServiceException e) {
      logger.warn("Place bid failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected place bid error", e);
      sendError(clientHandler, "Không thể đặt giá.");
    }
  }

  private void sendWalletUpdate(ClientHandler clientHandler, User user) {
    WalletUpdateResponse response = new WalletUpdateResponse(new UserData(user));
    clientHandler.sendPacket(PacketRes.of(true, PacketType.WALLET_UPDATE, "OK", response));
  }

  private void broadcastAuctionList(ClientHandler clientHandler) {
    try {
      List<AuctionSummary> summaries = auctionService.getAuctionSummaries();
      AuctionsResponse response = new AuctionsResponse(summaries);
      Server.broadcast(PacketRes.of(PacketType.FETCH_AUCTIONS, response), -1);
    } catch (Exception e) {
      logger.error("Failed to broadcast auction list", e);
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(PacketType.PLACE_BID, message));
  }
}
