package app.server.command;

import app.common.dto.AuctionDetailResponse;
import app.common.dto.CancelAuctionRequest;
import app.common.dto.CancelAuctionResponse;
import app.common.dto.WalletUpdateResponse;
import app.common.enums.ResponseType;
import app.common.exception.ServiceException;
import app.common.mapper.DtoMapper;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.AuctionService;
import app.server.service.UserService;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CancelAuctionCommand. */
public class CancelAuctionCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(CancelAuctionCommand.class);
  private final AuctionService auctionService;
  private final UserService userService;

  /** CancelAuctionCommand. */
  public CancelAuctionCommand(AuctionService auctionService, UserService userService) {
    this.auctionService = auctionService;
    this.userService = userService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    int auctionId = 0;
    try {
      CancelAuctionRequest request = packet.getData(CancelAuctionRequest.class);
      if (request == null || request.auctionId() <= 0 || request.expectedVersion() < 0) {
        sendError(clientHandler, auctionId, "Dữ liệu phiên đấu giá không hợp lệ.");
        return;
      }
      auctionId = request.auctionId();
      Set<Integer> releasedUserIds =
          auctionService.cancelAuction(
              auctionId, clientHandler.getUser().getId(), request.expectedVersion());
      clientHandler.sendPacket(
          PacketRes.of(
              ResponseType.CANCEL_AUCTION_RESULT,
              "Hủy phiên thành công.",
              new CancelAuctionResponse(auctionId)));
      Server.broadcast(
          PacketRes.of(
              ResponseType.AUCTION_CANCELLED,
              "Phiên đã bị hủy.",
              new CancelAuctionResponse(auctionId)),
          clientHandler.getUser().getId());
      Server.broadcastAuctionList(auctionService);
      Server.broadcastToAuctionViewers(
          auctionId,
          PacketRes.of(
              ResponseType.AUCTION_DETAIL_UPDATED,
              "OK",
              new AuctionDetailResponse(auctionService.getAuctionDetail(auctionId))),
          -1);
      sendWalletUpdates(releasedUserIds);
    } catch (ServiceException e) {
      logger.warn("Cancel auction failed: {}", e.getMessage());
      sendError(clientHandler, auctionId, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected cancel auction error", e);
      sendError(clientHandler, auctionId, "Không thể hủy phiên đấu giá.");
    }
  }

  private void sendError(ClientHandler clientHandler, int auctionId, String message) {
    clientHandler.sendPacket(PacketRes.error(ResponseType.CANCEL_AUCTION_RESULT, message));
  }

  private void sendWalletUpdates(Set<Integer> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return;
    }
    for (Integer userId : userIds) {
      try {
        var user = userService.getById(userId);
        Server.sendPacketToUser(
            userId,
            PacketRes.of(
                ResponseType.WALLET_UPDATED,
                "OK",
                new WalletUpdateResponse(DtoMapper.toUserData(user))));
      } catch (Exception e) {
        logger.warn("Failed to notify wallet update for user {}", userId, e);
      }
    }
  }
}
