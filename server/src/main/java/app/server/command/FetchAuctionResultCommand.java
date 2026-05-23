package app.server.command;

import app.common.dto.AuctionResultRequest;
import app.common.dto.AuctionResultResponse;
import app.common.dto.WalletUpdateResponse;
import app.common.enums.ResponseType;
import app.common.exception.ServiceException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FetchAuctionResultCommand. */
public class FetchAuctionResultCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(FetchAuctionResultCommand.class);
  private final AuctionService auctionService;
  private final UserService userService;

  /** FetchAuctionResultCommand. */
  public FetchAuctionResultCommand(AuctionService auctionService, UserService userService) {
    this.auctionService = auctionService;
    this.userService = userService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      AuctionResultRequest request = packet.getData(AuctionResultRequest.class);
      if (request == null) {
        sendError(clientHandler, "Dữ liệu yêu cầu không hợp lệ.");
        return;
      }
      int auctionId = request.auctionId();
      if (auctionId <= 0) {
        sendError(clientHandler, "auctionId không hợp lệ.");
        return;
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
      clientHandler.sendPacket(PacketRes.of(ResponseType.AUCTION_RESULT_FETCHED, "OK", response));
      sendWalletUpdates(completion);
    } catch (ServiceException e) {
      logger.warn("Fetch auction result failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("[SERVER] Fetch auction result error", e);
      sendError(clientHandler, "Không thể lấy kết quả đấu giá.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(ResponseType.AUCTION_RESULT_FETCHED, message));
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
