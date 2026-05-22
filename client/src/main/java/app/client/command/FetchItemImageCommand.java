// client/src/main/java/app/client/command/FetchItemImageCommand.java
package app.client.command;

import app.client.store.ItemStore;
import app.common.dto.FetchItemImageResponse;
import app.common.protocol.PacketRes;

/**
 * Nhận Base64 ảnh từ server, lưu vào ItemStore.imageBase64Cache. Sau notifyUpdate(), LiveController
 * sẽ tự đọc cache và render lên ImageView.
 */
public class FetchItemImageCommand extends Command {

  @Override
  public void execute(PacketRes packet) {
    if (packet.isSuccess()) {
      FetchItemImageResponse response = packet.getData(FetchItemImageResponse.class);
      if (response != null && response.base64Data() != null && !response.base64Data().isBlank()) {
        // Lưu vào cache — không đụng UI ở đây (đây là Socket listener thread)
        ItemStore.getInstance().setItemImageBase64(response.itemId(), response.base64Data());
      }
      // notifyUpdate() kích hoạt Platform.runLater trong LiveController
      notifyUpdate();
    }
    // Không notifyMessage để tránh trigger các listener khác (bid, detail...)
  }
}
