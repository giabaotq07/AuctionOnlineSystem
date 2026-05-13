package app.network;

import app.data.RegisterRequest;
import app.data.RegisterResponse;
import app.data.UserData;
import app.enums.PacketType;
import app.enums.UserRole;
import app.exception.ServiceException;
import app.models.Account;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.User;
import app.models.UserFactory;
import app.models.Wallet;
import app.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(RegisterCommand.class);
  private final UserService userService;

  public RegisterCommand(UserService userService) {
    this.userService = userService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    RegisterRequest request = packet.getData(RegisterRequest.class);
    if (request == null) {
      sendError(clientHandler, "Dữ liệu đăng ký không hợp lệ.");
      return;
    }
    String name = request.name();
    String username = request.account();
    String password = request.password();
    if (name == null
        || name.isBlank()
        || username == null
        || username.isBlank()
        || password == null
        || password.isBlank()) {
      sendError(clientHandler, "Thông tin đăng ký không được để trống.");
      return;
    }
    if (request.role() != null
        && request.role() != UserRole.BIDDER
        && request.role() != UserRole.SELLER) {
      sendError(clientHandler, "Vai trò không hợp lệ.");
      return;
    }
    UserRole role = request.role() != null ? request.role() : UserRole.BIDDER;
    try {
      User newUser =
          UserFactory.createUser(name, new Account(username, password), new Wallet(), role);
      User created = userService.register(newUser);
      logger.info("[SERVER] User {} registered successfully.", username);
      RegisterResponse response =
          new RegisterResponse(true, "Đăng ký thành công!", new UserData(created));
      clientHandler.sendPacket(PacketRes.of(true, PacketType.REGISTER, response));
    } catch (ServiceException e) {
      logger.warn("[SERVER] Register failed for user {}", username);
      sendError(clientHandler, "Tài khoản đã tồn tại hoặc dữ liệu không hợp lệ.");
    } catch (Exception e) {
      logger.error("[SERVER] Register error", e);
      sendError(clientHandler, "Lỗi hệ thống.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(
        PacketRes.of(false, PacketType.REGISTER, new RegisterResponse(false, message, null)));
  }
}
