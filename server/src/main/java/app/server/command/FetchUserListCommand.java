package app.server.command;

import app.common.dto.UserListResponse;
import app.common.enums.ResponseType;
import app.common.mapper.ModelMapper;
import app.common.models.User;
import app.common.protocol.PacketReq;
import app.server.network.ClientHandler;
import app.server.service.UserService;

/** FetchUserListCommand. */
public class FetchUserListCommand extends SafeCommand {
  private final UserService userService;

  /** FetchUserListCommand. */
  public FetchUserListCommand(UserService userService) {
    this.userService = userService;
  }

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) {
    java.util.List<User> users = userService.getAllUsers(requireUser(clientHandler).getId());
    sendSuccess(
        clientHandler,
        "OK",
        new UserListResponse(users.stream().map(ModelMapper::toUserPreview).toList()));
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.FETCH_USER_LIST_RESULT;
  }

  @Override
  protected String unexpectedErrorMessage() {
    return "Không thể tải danh sách người dùng.";
  }
}
