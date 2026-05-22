package app.server.command;

import app.common.dto.AuctionPaidNoticeResponse;
import app.common.dto.SettleWalletRequest;
import app.common.dto.WalletUpdateResponse;
import app.common.enums.ResponseType;
import app.common.exception.ServiceException;
import app.common.models.Auction;
import app.common.models.User;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.AuctionCompletion;
import app.server.service.AuctionQueryService;
import app.server.service.AuctionService;
import app.server.service.UserService;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SettleWalletCommand. */
public class SettleWalletCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(SettleWalletCommand.class);
  private final AuctionService auctionService;
  private final AuctionQueryService auctionQueryService;
  private final UserService userService;

  /** SettleWalletCommand. */
  public SettleWalletCommand(
      AuctionService auctionService,
      AuctionQueryService auctionQueryService,
      UserService userService) {
    this.auctionService = auctionService;
    this.auctionQueryService = auctionQueryService;
    this.userService = userService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      SettleWalletRequest request = packet.getData(SettleWalletRequest.class);
      if (request == null || request.auctionId() <= 0) {
        sendError(clientHandler, "Dữ liệu phiên đấu giá không hợp lệ.");
        return;
      }
      int auctionId = request.auctionId();
      int userId = clientHandler.getUser().getId();
      AuctionCompletion completion = auctionService.completeAuction(auctionId);
      BigDecimal paidAmount =
          completion.completed()
              ? completion.winningAmount()
              : auctionService.settleAuctionPayment(auctionId);
      Auction auction = auctionQueryService.getAuction(auctionId);
      if (paidAmount.signum() > 0 && auction.getWinnerId() != null) {
        String auctionName =
            auction.getItem() == null ? "Phiên #" + auctionId : auction.getItem().getName();
        AuctionPaidNoticeResponse sellerNotice =
            new AuctionPaidNoticeResponse(auctionId, auctionName, paidAmount, "SELLER");
        AuctionPaidNoticeResponse winnerNotice =
            new AuctionPaidNoticeResponse(auctionId, auctionName, paidAmount, "WINNER");
        Server.sendToUser(
            auction.getSellerId(),
            PacketRes.of(ResponseType.AUCTION_PAID_NOTICE, "OK", sellerNotice));
        Server.sendToUser(
            auction.getWinnerId(),
            PacketRes.of(ResponseType.AUCTION_PAID_NOTICE, "OK", winnerNotice));
      }
      Server.broadcastAuctionList(auctionQueryService);
      User updated = userService.getById(userId);
      WalletUpdateResponse response =
          new WalletUpdateResponse(app.common.mapper.ModelMapper.toUserDto(updated));
      clientHandler.sendPacket(
          PacketRes.of(ResponseType.WALLET_UPDATED, "Cập nhật ví thành công.", response));
    } catch (ServiceException e) {
      logger.warn("Settle wallet failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected settle wallet error", e);
      sendError(clientHandler, "Không thể cập nhật ví.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(ResponseType.SETTLE_WALLET_RESULT, message));
  }
}
