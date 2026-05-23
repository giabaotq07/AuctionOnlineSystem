package app.server.command;

import app.common.dto.LoginRequest;
import app.common.dto.LoginResponse;
import app.common.enums.ResponseType;
import app.common.exception.ValidationException;
import app.common.models.User;
import app.common.protocol.PacketReq;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.UserService;

/** LoginCommand. */
public class LoginCommand extends SafeCommand {
  private final UserService userService;

  /** LoginCommand. */
  public LoginCommand(UserService userService) {
    this.userService = userService;
  }

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) {
    LoginRequest request = requirePayload(packet, LoginRequest.class, "Dữ liệu đăng nhập không hợp lệ.");
    String username = request.username();
    String password = request.password();
    if (username == null || username.isBlank() || password == null || password.isBlank()) {
      throw new ValidationException("Tên đăng nhập và mật khẩu không được để trống.");
    }
    username = username.trim();
    User user = userService.login(username, password);
    clientHandler.getSession().authenticate(user);
    Server.registerClient(user.getId(), clientHandler);
    logger.info("[SERVER] User {} logged in", username);
    LoginResponse response = new LoginResponse(app.common.mapper.ModelMapper.toUserDto(user));
    sendSuccess(clientHandler, "Đăng nhập thành công!", response);
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.LOGIN_RESULT;
  }

  @Override
  protected String unexpectedErrorMessage() {
    return "Lỗi hệ thống.";
  }
}
