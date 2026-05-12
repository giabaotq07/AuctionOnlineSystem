package app.network;

import app.data.SettleWalletRequest;
import app.data.UserData;
import app.data.WalletUpdateResponse;
import app.enums.OperationStatus;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.User;
import app.service.AuctionService;
import app.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettleWalletCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(SettleWalletCommand.class);
  private final AuctionService auctionService;
  private final UserService userService;

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
      int winnerId = auctionService.getAuctionResult(auctionId).winner().id();
      boolean isWinner = winnerId == userId;
      User updated = userService.settleFrozenAmount(userId, auctionId, isWinner);
      String message = isWinner ? "Thanh toan thanh cong." : "Hoan tien thanh cong.";
      WalletUpdateResponse response =
          new WalletUpdateResponse(OperationStatus.SUCCESS, message, new UserData(updated));
      clientHandler.sendPacket(PacketRes.of(PacketType.WALLET_UPDATE, response));
    } catch (ServiceException e) {
      logger.warn("Settle wallet failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected settle wallet error", e);
      sendError(clientHandler, "Không thể cập nhật ví.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    WalletUpdateResponse response = new WalletUpdateResponse(OperationStatus.ERROR, message, null);
    clientHandler.sendPacket(PacketRes.of(PacketType.WALLET_UPDATE, response));
  }
}
