package app.network;

import app.data.AuctionSummary;
import app.data.AuctionsResponse;
import app.data.PlaceBidRequest;
import app.data.PlaceBidResponse;
import app.data.UserData;
import app.data.WalletUpdateResponse;
import app.enums.OperationStatus;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.User;
import app.service.BidService;
import app.service.UserService;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaceBidCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(PlaceBidCommand.class);
  private final BidService bidService;
  private final UserService userService;

  public PlaceBidCommand(BidService bidService, UserService userService) {
    this.bidService = bidService;
    this.userService = userService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    BigDecimal previousFrozen = null;
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
      // KHÔNG trust bidderId từ client
      bidderId = user.getId();
      previousFrozen =
          userService.reserveBidAmount(bidderId, auctionId, BigDecimal.valueOf(bidAmount));
      PlaceBidResponse response = bidService.placeBid(auctionId, bidderId, bidAmount);
      PacketRes packetResponse = PacketRes.of(PacketType.PLACE_BID, response);
      // sender
      clientHandler.sendPacket(packetResponse);
      // others
      Server.broadcast(packetResponse, bidderId);
      sendWalletUpdate(clientHandler, userService.getById(bidderId));
      // refresh auction list
      broadcastAuctionList(clientHandler);
      logger.info("User {} placed bid {} in auction {}", bidderId, bidAmount, auctionId);
    } catch (ServiceException e) {
      logger.warn("Place bid failed: {}", e.getMessage());
      rollbackFrozen(bidderId, auctionId, previousFrozen);
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected place bid error", e);
      rollbackFrozen(bidderId, auctionId, previousFrozen);
      sendError(clientHandler, "Không thể đặt giá.");
    }
  }

  private void rollbackFrozen(int bidderId, int auctionId, BigDecimal previousFrozen) {
    if (bidderId <= 0 || auctionId <= 0 || previousFrozen == null) {
      return;
    }
    try {
      userService.restoreFrozenAmount(bidderId, auctionId, previousFrozen);
    } catch (Exception e) {
      logger.warn("Failed to rollback frozen funds for user {}", bidderId, e);
    }
  }

  private void sendWalletUpdate(ClientHandler clientHandler, User user) {
    WalletUpdateResponse response =
        new WalletUpdateResponse(OperationStatus.SUCCESS, "OK", new UserData(user));
    clientHandler.sendPacket(PacketRes.of(PacketType.WALLET_UPDATE, response));
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
    WalletUpdateResponse response = new WalletUpdateResponse(OperationStatus.FAIL, message, null);
    clientHandler.sendPacket(PacketRes.of(PacketType.WALLET_UPDATE, response));
    clientHandler.sendPacket(PacketRes.error(PacketType.PLACE_BID, message));
  }
}
