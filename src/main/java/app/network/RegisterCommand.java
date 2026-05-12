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

public class RegisterCommand implements Command {

  private final UserService userService;

  public RegisterCommand(UserService userService) {
    this.userService = userService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    RegisterRequest request = packet.getData(RegisterRequest.class);
    UserRole role = request.role() != null ? request.role() : UserRole.BIDDER;
    User newUser =
        UserFactory.createUser(
            request.name(), new Account(request.account(), request.password()), new Wallet(), role);
    try {
      User created = userService.register(newUser);
      RegisterResponse response =
          new RegisterResponse(true, "Đăng ký thành công!", new UserData(created));
      clientHandler.sendMessage(PacketRes.of(PacketType.REGISTER, response));
    } catch (ServiceException e) {
      RegisterResponse response =
          new RegisterResponse(false, "Tài khoản đã tồn tại hoặc có lỗi xảy ra.", null);
      clientHandler.sendMessage(PacketRes.of(PacketType.REGISTER, response));
    }
  }
}
