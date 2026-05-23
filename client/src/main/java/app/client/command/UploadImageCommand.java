package app.client.command;

import app.client.store.ItemStore;
import app.common.dto.UploadImageResponse;
import app.common.models.Item;
import app.common.protocol.PacketRes;
import java.util.Optional;

/** Xử lý phản hồi UPLOAD_IMAGE từ server. */
public class UploadImageCommand extends Command {

  @Override
  public void execute(PacketRes packet) {
    if (packet.isSuccess()) {
      UploadImageResponse response = packet.getData(UploadImageResponse.class);
      if (response != null) {
        // Cập nhật imageUrl trong ItemStore local để UI phản ánh ngay
        Optional<Item> itemOpt = ItemStore.getInstance().findById(response.itemId());
        itemOpt.ifPresent(item -> item.setImageUrl(response.imagePath()));
      }
      notifyUpdate();
    }
//    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
