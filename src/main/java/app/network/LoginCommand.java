package app.network;

import app.dao.UserDAO;
import app.dto.LoginRequest;
import app.dto.LoginResponse;
import app.enums.PacketType;
import app.exception.AuthenticationException;
import app.models.Packet;
import app.models.User;
import app.service.UserService;

public class LoginCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, Packet packet) {
    LoginRequest loginPacket = (LoginRequest) packet.getData();
    String username = loginPacket.username();
    String password = loginPacket.password();
    UserDAO userDAO = new UserDAO();
    UserService userService = new UserService(userDAO);
    try {
      userService.login(username, password);
    } catch (AuthenticationException e) {
      LoginResponse LoginResponse =
          new LoginResponse(false, "Đăng nhập thất bại! Sai tên tài khoản hoặc mật khẩu.", null);
      Packet Packet = new Packet(PacketType.LOGIN, LoginResponse);
      clientHandler.sendMessage(Packet);
    }
    User user = userService.login(username, password);
    clientHandler.setUsername(username);
    Server.registerClient(username, clientHandler);
    System.out.println("[SERVER] " + username + " đã đăng nhập.");
    LoginResponse LoginResponse = new LoginResponse(true, "Đăng nhập thành công!", user);
    Packet Packet = new Packet(PacketType.LOGIN, LoginResponse);
    clientHandler.sendMessage(Packet);
  }
}
