package app.network;

import app.data.LoginRequest;
import app.data.LoginResponse;
import app.data.UserData;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.User;
import app.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(LoginCommand.class);
  private final UserService userService;

  public LoginCommand(UserService userService) {
    this.userService = userService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      LoginRequest request = packet.getData(LoginRequest.class);
      if (request == null) {
        sendError(clientHandler, "Dữ liệu đăng nhập không hợp lệ.");
        return;
      }
      String username = request.username();
      String password = request.password();
      if (username == null || username.isBlank() || password == null || password.isBlank()) {
        sendError(clientHandler, "Tên đăng nhập và mật khẩu không được để trống.");
        return;
      }
      username = username.trim();
      User user = userService.login(username, password);
      if (user == null) {
        sendError(clientHandler, "Sai tên tài khoản hoặc mật khẩu.");
        return;
      }
      clientHandler.getSession().authenticate(user);
      Server.registerClient(user.getId(), clientHandler);
      logger.info("[SERVER] User {} logged in", username);
      LoginResponse response = new LoginResponse(true, "Đăng nhập thành công!", new UserData(user));
      clientHandler.sendPacket(PacketRes.of(PacketType.LOGIN, response));
    } catch (ServiceException e) {
      logger.warn("[SERVER] Login failed: {}", e.getMessage());
      sendError(clientHandler, "Sai tên tài khoản hoặc mật khẩu.");
    } catch (Exception e) {
      logger.error("[SERVER] Login error", e);
      sendError(clientHandler, "Lỗi hệ thống.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(PacketType.LOGIN, message));
  }
}
