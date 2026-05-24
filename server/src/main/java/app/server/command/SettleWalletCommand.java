package app.server.command;

import app.common.dto.AuctionPaidNoticeResponse;
import app.common.dto.SettleWalletRequest;
import app.common.dto.WalletUpdateResponse;
import app.common.enums.ResponseType;
import app.common.exception.ValidationException;
import app.common.models.Auction;
import app.common.models.User;
import app.common.protocol.PacketReq;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.AuctionQueryService;
import app.server.service.AuctionService;
import app.server.service.UserService;
import app.server.service.result.AuctionCompletion;
import java.math.BigDecimal;

/** SettleWalletCommand. */
public class SettleWalletCommand extends SafeCommand {
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
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) {
    SettleWalletRequest request =
        requirePayload(packet, SettleWalletRequest.class, "Dữ liệu phiên đấu giá không hợp lệ.");
    if (request.auctionId() <= 0) {
      throw new ValidationException("Dữ liệu phiên đấu giá không hợp lệ.");
    }
    int auctionId = request.auctionId();
    int userId = requireUser(clientHandler).getId();
    AuctionCompletion completion = auctionService.completeAuction(auctionId);
    BigDecimal paidAmount =
        completion.completed()
            ? completion.winningAmount()
            : auctionService.settleAuctionPayment(auctionId);
    Auction auction = auctionQueryService.getAuction(auctionId);
    notifyPaidUsers(auctionId, auction, paidAmount);
    try {
      Server.broadcastAuctionList(auctionQueryService);
    } catch (Exception e) {
      logger.warn("Wallet settled for auction {}, but broadcast failed", auctionId, e);
    }
    User updated = userService.getById(userId);
    WalletUpdateResponse response =
        new WalletUpdateResponse(app.common.mapper.ModelMapper.toUserDto(updated));
    sendSuccess(clientHandler, "Cập nhật ví thành công.", response);
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.SETTLE_WALLET_RESULT;
  }

  @Override
  protected String unexpectedErrorMessage() {
    return "Không thể cập nhật ví.";
  }

  private void notifyPaidUsers(int auctionId, Auction auction, BigDecimal paidAmount) {
    if (paidAmount.signum() <= 0 || auction.getWinnerId() == null) {
      return;
    }
    try {
      String auctionName =
          auction.getItem() == null ? "Phiên #" + auctionId : auction.getItem().getName();
      AuctionPaidNoticeResponse sellerNotice =
          new AuctionPaidNoticeResponse(auctionId, auctionName, paidAmount, "SELLER");
      AuctionPaidNoticeResponse winnerNotice =
          new AuctionPaidNoticeResponse(auctionId, auctionName, paidAmount, "WINNER");
      Server.sendToUser(
          auction.getSellerId(),
          app.common.protocol.PacketRes.of(ResponseType.AUCTION_PAID_NOTICE, "OK", sellerNotice));
      Server.sendToUser(
          auction.getWinnerId(),
          app.common.protocol.PacketRes.of(ResponseType.AUCTION_PAID_NOTICE, "OK", winnerNotice));
    } catch (Exception e) {
      logger.warn("Auction {} was settled, but paid notification failed", auctionId, e);
    }
  }
}
