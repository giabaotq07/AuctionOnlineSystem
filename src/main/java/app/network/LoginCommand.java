package app.network;

import app.dao.UserDAO;
import app.dto.LoginRequest;
import app.dto.LoginResponse;
import app.enums.CommandType;
import app.models.MessagePacket;
import app.models.User;
import app.service.UserService;

public class LoginCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, MessagePacket<?> packet) {
    LoginRequest loginPacket = (LoginRequest) packet.getData();
    String username = loginPacket.username();
    String password = loginPacket.password();
    UserDAO userDAO = new UserDAO();
    UserService userService = new UserService(userDAO);
    if (userService.login(username, password)) {
      User user = userService.getUserByAccount(username);
      clientHandler.setUsername(username);
      Server.registerClient(username, clientHandler);
      System.out.println("[SERVER] " + username + " đã đăng nhập.");
      LoginResponse loginResponse = new LoginResponse(true, "Đăng nhập thành công!", user);
      MessagePacket<LoginResponse> responsePacket =
          new MessagePacket<>(CommandType.LOGIN, loginResponse);
      clientHandler.sendMessage(responsePacket);
    }  else {
      LoginResponse loginResponse = new LoginResponse(false, "Đăng nhập thất bại! Sai tên tài khoản hoặc mật khẩu.", null);
      MessagePacket<LoginResponse> responsePacket =
          new MessagePacket<>(CommandType.LOGIN, loginResponse);
      clientHandler.sendMessage(responsePacket);
    }
  }
}
