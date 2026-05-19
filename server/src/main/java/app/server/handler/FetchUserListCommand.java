package app.server.handler;

import app.common.dto.UserData;
import app.common.dto.UserListResponse;
import app.common.enums.PacketType;
import app.common.exception.ServiceException;
import app.common.mapper.DtoMapper;
import app.common.models.PacketReq;
import app.common.models.PacketRes;
import app.server.service.UserService;
import java.util.List;
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
      List<UserData> users =
          userService.getAllUsers(clientHandler.getUser().getId()).stream()
              .map(DtoMapper::toUserData)
              .toList();
      clientHandler.sendPacket(
          PacketRes.of(PacketType.FETCH_USER_LIST, new UserListResponse(users)));
    } catch (ServiceException e) {
      logger.warn("Fetch users failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected fetch users error", e);
      sendError(clientHandler, "Không thể tải danh sách người dùng.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(PacketType.FETCH_USER_LIST, message));
  }
}
