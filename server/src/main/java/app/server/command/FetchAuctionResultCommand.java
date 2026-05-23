package app.server.command;

import app.common.dto.AuctionResultRequest;
import app.common.dto.AuctionResultResponse;
import app.common.dto.WalletUpdateResponse;
import app.common.enums.ResponseType;
import app.common.exception.ValidationException;
import app.common.models.Bid;
import app.common.models.User;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.AuctionService;
import app.server.service.UserService;
import app.server.service.result.AuctionCompletion;
import java.util.Optional;

/** FetchAuctionResultCommand. */
public class FetchAuctionResultCommand extends SafeCommand {
  private final AuctionService auctionService;
  private final UserService userService;

  /** FetchAuctionResultCommand. */
  public FetchAuctionResultCommand(AuctionService auctionService, UserService userService) {
    this.auctionService = auctionService;
    this.userService = userService;
  }

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) {
    AuctionResultRequest request =
        requirePayload(packet, AuctionResultRequest.class, "Dữ liệu yêu cầu không hợp lệ.");
    int auctionId = request.auctionId();
    if (auctionId <= 0) {
      throw new ValidationException("auctionId không hợp lệ.");
    }
    AuctionCompletion completion = auctionService.completeAuction(auctionId);
    Optional<Bid> highestBid = completion.highestBid();
    AuctionResultResponse response =
        new AuctionResultResponse(
            auctionId,
            highestBid
                .map(Bid::getBidder)
                .map(app.common.mapper.ModelMapper::toUserDto)
                .orElse(null),
            highestBid.map(Bid::getAmount).orElse(0L));
    sendSuccess(clientHandler, "OK", response);
    try {
      sendWalletUpdates(completion);
    } catch (Exception e) {
      logger.warn("Auction {} result fetched, but wallet notification failed", auctionId, e);
    }
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.AUCTION_RESULT_FETCHED;
  }

  @Override
  protected String unexpectedErrorMessage() {
    return "Không thể lấy kết quả đấu giá.";
  }

  private void sendWalletUpdates(AuctionCompletion completion) {
    if (completion == null || !completion.completed()) {
      return;
    }
    for (Integer userId : completion.settledUserIds()) {
      User user = userService.getById(userId);
      Server.sendPacketToUser(
          userId,
          PacketRes.of(
              ResponseType.WALLET_UPDATED,
              "OK",
              new WalletUpdateResponse(app.common.mapper.ModelMapper.toUserDto(user))));
    }
  }
}
