package app.network;

import app.dao.UserDAO;
import app.dao.impl.MySqlUserDAO;
import app.data.RegisterRequest;
import app.data.RegisterResponse;
import app.data.UserData;
import app.enums.PacketType;
import app.enums.UserRole;
import app.exception.DatabaseException;
import app.exception.ServiceException;
import app.models.Account;
import app.models.Packet;
import app.models.User;
import app.models.UserFactory;
import app.models.Wallet;
import app.service.UserService;
import app.utils.JsonUtil;

public class RegisterCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, Packet packet) {
    RegisterRequest request = JsonUtil.fromJson(packet.getData(), RegisterRequest.class);
    UserRole role = request.role() != null ? request.role() : UserRole.BIDDER;
    User newUser =
        UserFactory.createUser(
            request.name(), new Account(request.account(), request.password()), new Wallet(), role);
    UserDAO userDAO = new MySqlUserDAO();
    UserService userService = new UserService(userDAO);
    try {
      User created = userService.register(newUser);
      RegisterResponse response =
          new RegisterResponse(true, "Đăng ký thành công!", new UserData(created));
      clientHandler.sendMessage(new Packet(PacketType.REGISTER, JsonUtil.toJson(response)));
    } catch (DatabaseException | ServiceException e) {
      RegisterResponse response =
          new RegisterResponse(false, "Tài khoản đã tồn tại hoặc có lỗi xảy ra.", null);
      clientHandler.sendMessage(new Packet(PacketType.REGISTER, JsonUtil.toJson(response)));
    }
  }
}
