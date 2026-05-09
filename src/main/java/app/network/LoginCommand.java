package app.network;

import app.dao.UserDAO;
import app.dao.impl.MySqlUserDAO;
import app.data.LoginRequest;
import app.data.LoginResponse;
import app.data.UserData;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.models.Packet;
import app.models.User;
import app.service.UserService;
import app.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(LoginCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Packet packet) {
    LoginRequest loginRequest = JsonUtil.fromJson(packet.getData(), LoginRequest.class);
    String username = loginRequest.username();
    String password = loginRequest.password();
    UserDAO userDAO = new MySqlUserDAO();
    UserService userService = new UserService(userDAO);
    User user;
    try {
      user = userService.login(username, password);
    } catch (ServiceException e) {
      LoginResponse loginResponse =
          new LoginResponse(false, "Đăng nhập thất bại! Sai tên tài khoản hoặc mật khẩu.", null);
      Packet responsePacket = new Packet(PacketType.LOGIN, JsonUtil.toJsonElement(loginResponse));
      clientHandler.sendMessage(responsePacket);
      return;
    }
    clientHandler.setUser(user);
    Server.registerClient(user.getId(), clientHandler);
    logger.info("[SERVER] {} đã đăng nhập.", username);
    LoginResponse loginResponse =
        new LoginResponse(true, "Đăng nhập thành công!", new UserData(user));
    Packet responsePacket = new Packet(PacketType.LOGIN, JsonUtil.toJsonElement(loginResponse));
    clientHandler.sendMessage(responsePacket);
  }
}
