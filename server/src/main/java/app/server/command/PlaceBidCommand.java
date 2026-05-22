package app.server.command;

import app.common.dto.*;
import app.common.enums.ResponseType;
import app.common.exception.ServiceException;
import app.common.models.*;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.AuctionQueryService;
import app.server.service.BidService;
import app.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** PlaceBidCommand. */
public class PlaceBidCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(PlaceBidCommand.class);
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
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    int auctionId = 0;
    int bidderId = 0;
    try {
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
      bidderId = user.getId();
      Auction updatedAuction = bidService.placeBid(auctionId, user, bidAmount);
      PlaceBidResponse response =
          new PlaceBidResponse(updatedAuction.getId(), updatedAuction.getHighestBid(), bidderId);
      PacketRes packetResponse =
          PacketRes.of(ResponseType.PLACE_BID_RESULT, "Đặt giá thành công.", response);
      clientHandler.sendPacket(packetResponse);
      Server.broadcastToAuctionViewers(
          auctionId,
          PacketRes.of(ResponseType.BID_PLACED, "Có lượt đặt giá mới.", response),
          bidderId);
      sendWalletUpdate(clientHandler, userService.getById(bidderId));
      Server.broadcastAuctionList(auctionQueryService);
      broadcastAuctionDetail(auctionId);
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
    WalletUpdateResponse response =
        new WalletUpdateResponse(app.common.mapper.ModelMapper.toUserDto(user));
    clientHandler.sendPacket(PacketRes.of(ResponseType.WALLET_UPDATED, "OK", response));
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

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(ResponseType.PLACE_BID_RESULT, message));
  }
}
