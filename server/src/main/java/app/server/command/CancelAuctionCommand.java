package app.server.command;

import app.common.dto.AuctionDetailResponse;
import app.common.dto.CancelAuctionRequest;
import app.common.dto.CancelAuctionResponse;
import app.common.dto.WalletUpdateResponse;
import app.common.enums.ResponseType;
import app.common.exception.ValidationException;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.AuctionQueryService;
import app.server.service.AuctionService;
import app.server.service.UserService;
import java.util.Set;

/** CancelAuctionCommand. */
public class CancelAuctionCommand extends SafeCommand {
  private final AuctionService auctionService;
  private final AuctionQueryService auctionQueryService;
  private final UserService userService;

  /** CancelAuctionCommand. */
  public CancelAuctionCommand(
      AuctionService auctionService,
      AuctionQueryService auctionQueryService,
      UserService userService) {
    this.auctionService = auctionService;
    this.auctionQueryService = auctionQueryService;
    this.userService = userService;
  }

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) {
    CancelAuctionRequest request =
        requirePayload(packet, CancelAuctionRequest.class, "Dữ liệu phiên đấu giá không hợp lệ.");
    if (request.auctionId() <= 0 || request.expectedVersion() < 0) {
      throw new ValidationException("Dữ liệu phiên đấu giá không hợp lệ.");
    }
    var user = requireUser(clientHandler);
    int auctionId = request.auctionId();
    Set<Integer> releasedUserIds =
        auctionService.cancelAuction(auctionId, user, request.expectedVersion());
    sendSuccess(clientHandler, "Hủy phiên thành công.", new CancelAuctionResponse(auctionId));
    notifyAfterCancel(auctionId, user.getId(), releasedUserIds);
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.CANCEL_AUCTION_RESULT;
  }

  @Override
  protected String unexpectedErrorMessage() {
    return "Không thể hủy phiên đấu giá.";
  }

  private void notifyAfterCancel(int auctionId, int actorId, Set<Integer> releasedUserIds) {
    try {
      Server.broadcast(
          PacketRes.of(
              ResponseType.AUCTION_CANCELLED,
              "Phiên đã bị hủy.",
              new CancelAuctionResponse(auctionId)),
          actorId);
      Server.broadcastAuctionList(auctionQueryService);
      Server.broadcastToAuctionViewers(
          auctionId,
          PacketRes.of(
              ResponseType.AUCTION_DETAIL_UPDATED,
              "OK",
              new AuctionDetailResponse(
                  app.common.mapper.ModelMapper.toAuctionDto(
                      auctionQueryService.getAuctionDetail(auctionId)))),
          -1);
    } catch (Exception e) {
      logger.warn("Auction {} was canceled, but post-cancel notification failed", auctionId, e);
    }
    sendWalletUpdates(releasedUserIds);
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
                new WalletUpdateResponse(app.common.mapper.ModelMapper.toUserDto(user))));
      } catch (Exception e) {
        logger.warn("Failed to notify wallet update for user {}", userId, e);
      }
    }
  }
}
