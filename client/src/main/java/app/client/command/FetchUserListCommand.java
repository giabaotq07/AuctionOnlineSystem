package app.client.command;

import app.client.store.UserListStore;
import app.common.dto.UserListResponse;
import app.common.protocol.PacketRes;

/** FetchUserListCommand handles incoming user list response. */
public class FetchUserListCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet.isSuccess()) {
      UserListResponse response = packet.getData(UserListResponse.class);
      if (response != null && response.users() != null) {
        UserListStore.getInstance().setMasterUsers(response.users());
        notifyUserListUpdate();
      }
    } else {
      notifyMessage(packet == null ? "Không thể tải danh sách người dùng." : packet.getMessage());
    }
  }
}
