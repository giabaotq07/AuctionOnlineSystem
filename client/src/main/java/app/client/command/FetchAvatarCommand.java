package app.client.command;

import app.client.manager.UserManager;
import app.common.dto.FetchAvatarResponse;
import app.common.protocol.PacketRes;

/** Xử lý phản hồi FETCH_AVATAR từ server. */
public class FetchAvatarCommand extends Command {

  @Override
  public void execute(PacketRes packet) {
    if (packet.isSuccess()) {
      FetchAvatarResponse response = packet.getData(FetchAvatarResponse.class);
      if (response != null && response.base64Data() != null && !response.base64Data().isBlank()) {
        UserManager.getInstance()
            .setAvatarBase64(response.userId(), response.avatarUrl(), response.base64Data());
      }
      notifyUpdate();
    }
  }
}
