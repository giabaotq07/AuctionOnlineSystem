package app.server.command;

import app.common.dto.DepositRequest;
import app.common.dto.WalletUpdateResponse;
import app.common.enums.ResponseType;
import app.common.models.User;
import app.common.protocol.PacketReq;
import app.server.network.ClientHandler;
import app.server.service.UserService;
import java.math.BigDecimal;

/** DepositCommand. */
public class DepositCommand extends SafeCommand {
  private final UserService userService;

  /** DepositCommand. */
  public DepositCommand(UserService userService) {
    this.userService = userService;
  }

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) {
    DepositRequest request =
        requirePayload(packet, DepositRequest.class, "Dữ liệu nạp tiền không hợp lệ.");
    if (request.amount() == null) {
      throw new app.common.exception.ValidationException("Dữ liệu nạp tiền không hợp lệ.");
    }
    BigDecimal amount = request.amount();
    User user = userService.deposit(requireUser(clientHandler).getId(), amount);
    WalletUpdateResponse response =
        new WalletUpdateResponse(app.common.mapper.ModelMapper.toUserDto(user));
    sendSuccess(clientHandler, "OK", response);
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.DEPOSIT_RESULT;
  }

  @Override
  protected String unexpectedErrorMessage() {
    return "Không thể nạp tiền.";
  }
}
