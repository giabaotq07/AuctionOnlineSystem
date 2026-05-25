package app.client.command;

import app.client.store.UserListStore;
import app.common.dto.BanUserResponse;
import app.common.protocol.PacketRes;

public class BanUserCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet != null && packet.isSuccess()) {
      BanUserResponse response = packet.getData(BanUserResponse.class);
      if (response == null) {
        notifyMessage(packet.getMessage());
        return;
      }
      UserListStore.getInstance().updateUserBannedStatus(response.userId(), response.isBanned());
      notifyUserListUpdate();
    }
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
