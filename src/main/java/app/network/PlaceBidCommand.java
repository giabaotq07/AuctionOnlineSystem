package app.network;

import app.dto.AuctionDetailResponse;
import app.dto.AuctionSummariesResponse;
import app.dto.AuctionSummary;
import app.dto.PlaceBidRequest;
import app.dto.PlaceBidResponse;
import app.dto.WalletUpdateResponse;
import app.enums.PacketType;
import app.enums.UserRole;
import app.exception.ServiceException;
import app.mapper.DtoMapper;
import app.models.Auction;
import app.models.Item;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.User;
import app.service.AuctionService;
import app.service.BidService;
import app.service.ItemService;
import app.service.UserService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** PlaceBidCommand. */
public class PlaceBidCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(PlaceBidCommand.class);
  private final BidService bidService;
  private final UserService userService;
  private final AuctionService auctionService;
  private final ItemService itemService;

  /** PlaceBidCommand. */
  public PlaceBidCommand(
      BidService bidService,
      UserService userService,
      AuctionService auctionService,
      ItemService itemService) {
    this.bidService = bidService;
    this.userService = userService;
    this.auctionService = auctionService;
    this.itemService = itemService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
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
      if (user.getRole() != UserRole.BIDDER) {
        sendError(clientHandler, "Chỉ Bidder được đặt giá.");
        return;
      }
      bidderId = user.getId();
      Auction updatedAuction = bidService.placeBid(auctionId, bidderId, bidAmount);
      PlaceBidResponse response =
          new PlaceBidResponse(updatedAuction.getId(), updatedAuction.getHighestBid(), bidderId);
      auctionService.invalidateCache();
      PacketRes packetResponse =
          PacketRes.of(true, PacketType.PLACE_BID, "Đặt giá thành công.", response);
      clientHandler.sendPacket(packetResponse);
      Server.broadcast(packetResponse, bidderId);
      sendWalletUpdate(clientHandler, userService.getById(bidderId));
      broadcastAuctionSumList(clientHandler);
      broadcastAuctionDetail(clientHandler, auctionId);
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
    WalletUpdateResponse response = new WalletUpdateResponse(DtoMapper.toUserData(user));
    clientHandler.sendPacket(PacketRes.of(true, PacketType.WALLET_UPDATE, "OK", response));
  }

  private void broadcastAuctionSumList(ClientHandler clientHandler) {
    try {
      List<AuctionSummary> summaries =
          auctionService.getAllAuctions().stream().map(this::toSummary).toList();
      AuctionSummariesResponse response = new AuctionSummariesResponse(summaries);
      Server.broadcast(PacketRes.of(PacketType.FETCH_AUCTION_SUMMARIES, response), -1);
    } catch (Exception e) {
      logger.error("Failed to broadcast auction list", e);
    }
  }

  private void broadcastAuctionDetail(ClientHandler clientHandler, int auctionId) {
    try {
      Auction auction = auctionService.getAuctionById(auctionId);
      Item item = requireItem(auction);
      AuctionDetailResponse response =
          new AuctionDetailResponse(DtoMapper.toAuctionDetail(auction, item));
      Server.broadcast(PacketRes.of(PacketType.FETCH_AUCTION_DETAIL, response), -1);
    } catch (Exception e) {
      logger.error("Failed to broadcast auction detail", e);
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(PacketType.PLACE_BID, message));
  }

  private AuctionSummary toSummary(Auction auction) {
    return DtoMapper.toAuctionSummary(auction, requireItem(auction));
  }

  private Item requireItem(Auction auction) {
    return itemService
        .getById(auction.getItemId())
        .orElseThrow(() -> new ServiceException("Không tìm thấy vật phẩm."));
  }
}
