package app.client.command;

import app.client.manager.UserManager;
import app.common.dto.UploadAvatarResponse;
import app.common.models.User;
import app.common.protocol.PacketRes;

/** Xử lý phản hồi UPLOAD_AVATAR từ server. */
public class UploadAvatarCommand extends Command {

  @Override
  public void execute(PacketRes packet) {
    if (packet.isSuccess()) {
      UploadAvatarResponse response = packet.getData(UploadAvatarResponse.class);
      if (response != null) {
        User currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser != null) {
          currentUser.setAvatarUrl(response.avatarUrl());
        }
      }
      notifyUpdate();
    }
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
