package app.network;

import app.data.DepositRequest;
import app.data.UserData;
import app.data.WalletUpdateResponse;
import app.enums.OperationStatus;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.User;
import app.service.UserService;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DepositCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(DepositCommand.class);
  private final UserService userService;

  public DepositCommand(UserService userService) {
    this.userService = userService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      if (!clientHandler.isAuthenticated()) {
        sendError(clientHandler, "Authentication required");
        return;
      }
      DepositRequest request = packet.getData(DepositRequest.class);
      if (request == null || request.amount() == null) {
        sendError(clientHandler, "Dữ liệu nạp tiền không hợp lệ.");
        return;
      }
      BigDecimal amount = request.amount();
      User user = userService.deposit(clientHandler.getUser().getId(), amount);
      WalletUpdateResponse response =
          new WalletUpdateResponse(OperationStatus.SUCCESS, "OK", new UserData(user));
      clientHandler.sendPacket(PacketRes.of(PacketType.WALLET_UPDATE, response));
    } catch (ServiceException e) {
      logger.warn("Deposit failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected deposit error", e);
      sendError(clientHandler, "Không thể nạp tiền.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    WalletUpdateResponse response = new WalletUpdateResponse(OperationStatus.FAIL, message, null);
    clientHandler.sendPacket(PacketRes.of(PacketType.WALLET_UPDATE, response));
  }
}
