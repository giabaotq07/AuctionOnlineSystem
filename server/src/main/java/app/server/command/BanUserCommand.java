package app.server.command;

import app.common.dto.BanUserRequest;
import app.common.dto.BanUserResponse;
import app.common.enums.ResponseType;
import app.common.protocol.PacketReq;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.UserService;

public class BanUserCommand extends SafeCommand {
  private final UserService userService;

  public BanUserCommand(UserService userService) {
    this.userService = userService;
  }

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) throws Exception {
    BanUserRequest request = requirePayload(packet, BanUserRequest.class, "Dữ liệu không hợp lệ.");
    if (request.userId() <= 0) {
      throw new IllegalArgumentException("Dữ liệu không hợp lệ.");
    }
    requireUser(clientHandler);
    if (clientHandler.getUser().getId() == request.userId()) {
      throw new IllegalArgumentException("Không thể tự cấm chính mình.");
    }
    if (request.ban()) {
      userService.banUser(request.userId());
    } else {
      userService.unbanUser(request.userId());
    }
    Server.updateOnlineUserStatus(request.userId(), !request.ban());
    sendSuccess(
        clientHandler,
        request.ban() ? "Cấm tài khoản thành công." : "Mở khóa tài khoản thành công.",
        new BanUserResponse(request.userId(), request.ban()));
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.USER_BANNED_NOTICE;
  }
}
