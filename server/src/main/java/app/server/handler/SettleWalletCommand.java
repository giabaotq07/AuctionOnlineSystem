package app.server.handler;

import app.common.dto.AuctionPaidNoticeResponse;
import app.common.dto.AuctionSummariesResponse;
import app.common.dto.SettleWalletRequest;
import app.common.dto.WalletUpdateResponse;
import app.common.enums.PacketType;
import app.common.exception.ServiceException;
import app.common.mapper.DtoMapper;
import app.common.models.PacketReq;
import app.common.models.PacketRes;
import app.common.models.User;
import app.server.service.AuctionService;
import app.server.service.AuctionSnapshot;
import app.server.service.UserService;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SettleWalletCommand. */
public class SettleWalletCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(SettleWalletCommand.class);
  private final AuctionService auctionService;
  private final UserService userService;

  /** SettleWalletCommand. */
  public SettleWalletCommand(AuctionService auctionService, UserService userService) {
    this.auctionService = auctionService;
    this.userService = userService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      if (!clientHandler.isAuthenticated()) {
        sendError(clientHandler, "Authentication required");
        return;
      }
      SettleWalletRequest request = packet.getData(SettleWalletRequest.class);
      if (request == null || request.auctionId() <= 0) {
        sendError(clientHandler, "Dữ liệu phiên đấu giá không hợp lệ.");
        return;
      }
      int auctionId = request.auctionId();
      int userId = clientHandler.getUser().getId();
      auctionService.completeAndGetHighestBid(auctionId);
      AuctionSnapshot snapshot = auctionService.getAuction(auctionId);
      BigDecimal paidAmount = auctionService.settleAuctionPayment(auctionId);
      if (paidAmount.signum() > 0 && snapshot.auction().getWinnerId() != null) {
        String auctionName = snapshot.item() == null ? "Phiên #" + auctionId : snapshot.item().getName();
        AuctionPaidNoticeResponse sellerNotice =
            new AuctionPaidNoticeResponse(auctionId, auctionName, paidAmount, "SELLER");
        AuctionPaidNoticeResponse winnerNotice =
            new AuctionPaidNoticeResponse(auctionId, auctionName, paidAmount, "WINNER");
        Server.sendToUser(
            snapshot.auction().getSellerId(),
            PacketRes.of(true, PacketType.AUCTION_PAID_NOTICE, sellerNotice));
        Server.sendToUser(
            snapshot.auction().getWinnerId(),
            PacketRes.of(true, PacketType.AUCTION_PAID_NOTICE, winnerNotice));
      }
      List<AuctionSnapshot> snapshots = auctionService.getAuctions();
      AuctionSummariesResponse summariesResponse =
          new AuctionSummariesResponse(
              snapshots.stream()
                  .map(snapshotItem -> DtoMapper.toAuctionSummary(snapshotItem.auction(), snapshotItem.item()))
                  .toList());
      Server.broadcast(
          PacketRes.of(true, PacketType.FETCH_AUCTION_SUMMARIES, summariesResponse), -1);
      User updated = userService.getById(userId);
      WalletUpdateResponse response = new WalletUpdateResponse(DtoMapper.toUserData(updated));
      clientHandler.sendPacket(
          PacketRes.of(true, PacketType.WALLET_UPDATE, "Cập nhật ví thành công.", response));
    } catch (ServiceException e) {
      logger.warn("Settle wallet failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected settle wallet error", e);
      sendError(clientHandler, "Không thể cập nhật ví.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(PacketType.WALLET_UPDATE, message));
  }
}
