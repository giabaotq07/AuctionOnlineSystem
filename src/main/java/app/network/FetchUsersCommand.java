package app.network;

import app.data.UserData;
import app.data.UsersResponse;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.models.PacketReq;
import app.models.PacketRes;
import app.service.UserService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FetchUsersCommand. */
public class FetchUsersCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(FetchUsersCommand.class);
  private final UserService userService;

  /** FetchUsersCommand. */
  public FetchUsersCommand(UserService userService) {
    this.userService = userService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      List<UserData> users =
          userService.getAllUsers(clientHandler.getUser().getId()).stream()
              .map(UserData::new)
              .toList();
      clientHandler.sendPacket(
          PacketRes.of(PacketType.FETCH_USERS, new UsersResponse(true, "OK", users)));
    } catch (ServiceException e) {
      logger.warn("Fetch users failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected fetch users error", e);
      sendError(clientHandler, "Không thể tải danh sách người dùng.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(
        PacketRes.of(false, PacketType.FETCH_USERS, new UsersResponse(false, message, List.of())));
  }
}
