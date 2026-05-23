package app.server.command;

import app.common.dto.*;
import app.common.enums.ResponseType;
import app.common.exception.ValidationException;
import app.common.models.*;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.AuctionQueryService;
import app.server.service.BidService;
import app.server.service.UserService;

/** PlaceBidCommand. */
public class PlaceBidCommand extends SafeCommand {
  private final BidService bidService;
  private final UserService userService;
  private final AuctionQueryService auctionQueryService;

  /** PlaceBidCommand. */
  public PlaceBidCommand(
      BidService bidService, UserService userService, AuctionQueryService auctionQueryService) {
    this.bidService = bidService;
    this.userService = userService;
    this.auctionQueryService = auctionQueryService;
  }

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) {
    PlaceBidRequest request =
        requirePayload(packet, PlaceBidRequest.class, "Dữ liệu đặt giá không hợp lệ.");
    int auctionId = request.auctionId();
    long bidAmount = request.bidAmount();
    if (auctionId <= 0) {
      throw new ValidationException("Phiên đấu giá không hợp lệ.");
    }
    User user = requireUser(clientHandler);
    int bidderId = user.getId();
    Auction updatedAuction = bidService.placeBid(auctionId, user, bidAmount);
    PlaceBidResponse response =
        new PlaceBidResponse(updatedAuction.getId(), updatedAuction.getHighestBid(), bidderId);
    sendSuccess(clientHandler, "Đặt giá thành công.", response);
    notifyAfterBid(clientHandler, auctionId, bidderId, response);
    logger.info("User {} placed bid {} in auction {}", bidderId, bidAmount, auctionId);
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.PLACE_BID_RESULT;
  }

  @Override
  protected String unexpectedErrorMessage() {
    return "Không thể đặt giá.";
  }

  private void sendWalletUpdate(ClientHandler clientHandler, User user) {
    WalletUpdateResponse response =
        new WalletUpdateResponse(app.common.mapper.ModelMapper.toUserDto(user));
    clientHandler.sendPacket(PacketRes.of(ResponseType.WALLET_UPDATED, "OK", response));
  }

  private void notifyAfterBid(
      ClientHandler clientHandler, int auctionId, int bidderId, PlaceBidResponse response) {
    try {
      sendWalletUpdate(clientHandler, userService.getById(bidderId));
    } catch (Exception e) {
      logger.warn("Bid in auction {} succeeded, but wallet notification failed", auctionId, e);
    }
    try {
      Server.broadcastToAuctionViewers(
          auctionId,
          PacketRes.of(ResponseType.BID_PLACED, "Có lượt đặt giá mới.", response),
          bidderId);
      Server.broadcastAuctionList(auctionQueryService);
      broadcastAuctionDetail(auctionId);
    } catch (Exception e) {
      logger.warn("Bid in auction {} succeeded, but auction notification failed", auctionId, e);
    }
  }

  private void broadcastAuctionDetail(int auctionId) {
    try {
      AuctionDetailResponse response =
          new AuctionDetailResponse(
              app.common.mapper.ModelMapper.toAuctionDto(
                  auctionQueryService.getAuctionDetail(auctionId)));
      Server.broadcastToAuctionViewers(
          auctionId, PacketRes.of(ResponseType.AUCTION_DETAIL_UPDATED, "OK", response), -1);
    } catch (Exception e) {
      logger.error("Failed to broadcast auction detail", e);
    }
  }
}
