package app.server.command;

import app.common.dto.RegisterRequest;
import app.common.dto.RegisterResponse;
import app.common.enums.ResponseType;
import app.common.enums.UserRole;
import app.common.exception.ValidationException;
import app.common.models.*;
import app.common.protocol.PacketReq;
import app.server.network.ClientHandler;
import app.server.service.UserService;

/** RegisterCommand. */
public class RegisterCommand extends SafeCommand {
  private final UserService userService;

  /** RegisterCommand. */
  public RegisterCommand(UserService userService) {
    this.userService = userService;
  }

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) {
    RegisterRequest request =
        requirePayload(packet, RegisterRequest.class, "Dữ liệu đăng ký không hợp lệ.");
    String name = request.name();
    String username = request.account();
    String password = request.password();
    if (name == null
        || name.isBlank()
        || username == null
        || username.isBlank()
        || password == null
        || password.isBlank()) {
      throw new ValidationException("Thông tin đăng ký không được để trống.");
    }
    if (request.role() != null
        && request.role() != UserRole.BIDDER
        && request.role() != UserRole.SELLER) {
      throw new ValidationException("Vai trò không hợp lệ.");
    }
    UserRole role = request.role() != null ? request.role() : UserRole.BIDDER;
    User newUser = new User(name, new Account(username, password, role), new Wallet());
    User created = userService.register(newUser);
    logger.info("[SERVER] User {} registered successfully.", username);
    RegisterResponse response =
        new RegisterResponse(app.common.mapper.ModelMapper.toUserDto(created));
    sendSuccess(clientHandler, "Đăng ký thành công!", response);
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.REGISTER_RESULT;
  }

  @Override
  protected String unexpectedErrorMessage() {
    return "Lỗi hệ thống.";
  }
}
