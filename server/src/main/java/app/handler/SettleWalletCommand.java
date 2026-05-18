package app.handler;

import app.dto.SettleWalletRequest;
import app.dto.WalletUpdateResponse;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.mapper.DtoMapper;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.User;
import app.service.AuctionService;
import app.service.UserService;
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
