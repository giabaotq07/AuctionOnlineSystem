package app.server.command;

import app.common.dto.UserListResponse;
import app.common.dto.UserPreview;
import app.common.enums.ResponseType;
import app.common.exception.ServiceException;
import app.common.models.User;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FetchUserListCommand. */
public class FetchUserListCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(FetchUserListCommand.class);
  private final UserService userService;

  /** FetchUserListCommand. */
  public FetchUserListCommand(UserService userService) {
    this.userService = userService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      java.util.List<User> users = userService.getAllUsers(clientHandler.getUser().getId());
      clientHandler.sendPacket(
          PacketRes.of(
              ResponseType.FETCH_USER_LIST_RESULT,
              "OK",
              new UserListResponse(users.stream().map(UserPreview::from).toList())));
    } catch (ServiceException e) {
      logger.warn("Fetch users failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected fetch users error", e);
      sendError(clientHandler, "Không thể tải danh sách người dùng.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(ResponseType.FETCH_USER_LIST_RESULT, message));
  }
}
