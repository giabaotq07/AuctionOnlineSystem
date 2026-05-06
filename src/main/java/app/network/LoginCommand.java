package app.network;

import app.dao.UserDAO;
import app.dao.impl.MySqlUserDAO;
import app.dto.LoginRequest;
import app.dto.LoginResponse;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.models.Packet;
import app.models.User;
import app.service.UserService;

public class LoginCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, Packet packet) {
    LoginRequest loginPacket = (LoginRequest) packet.getData();
    String username = loginPacket.username();
    String password = loginPacket.password();
    UserDAO userDAO = new MySqlUserDAO();
    UserService userService = new UserService(userDAO);
    User user;
    try {
      user = userService.login(username, password);
    } catch (ServiceException e) {
      LoginResponse loginResponse =
          new LoginResponse(false, "Đăng nhập thất bại! Sai tên tài khoản hoặc mật khẩu.", null);
      Packet responsePacket = new Packet(PacketType.LOGIN, loginResponse);
      clientHandler.sendMessage(responsePacket);
      return;
    }
    clientHandler.setUsername(username);
    Server.registerClient(username, clientHandler);
    System.out.println("[SERVER] " + username + " đã đăng nhập.");
    LoginResponse loginResponse = new LoginResponse(true, "Đăng nhập thành công!", user);
    Packet responsePacket = new Packet(PacketType.LOGIN, loginResponse);
    clientHandler.sendMessage(responsePacket);
  }
}
