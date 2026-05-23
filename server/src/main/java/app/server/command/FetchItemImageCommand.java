// server/src/main/java/app/server/command/FetchItemImageCommand.java
package app.server.command;

import app.common.dto.FetchItemImageRequest;
import app.common.dto.FetchItemImageResponse;
import app.common.enums.ResponseType;
import app.common.exception.ValidationException;
import app.common.models.Item;
import app.common.protocol.PacketReq;
import app.server.network.ClientHandler;
import app.server.service.ImageStorageService;
import app.server.service.ItemService;

/** Đọc file ảnh từ server_data, encode Base64, gửi về client qua Socket. */
public class FetchItemImageCommand extends SafeCommand {
  private final ImageStorageService imageStorageService;
  private final ItemService itemService;

  public FetchItemImageCommand(ImageStorageService imageStorageService, ItemService itemService) {
    this.imageStorageService = imageStorageService;
    this.itemService = itemService;
  }

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet)
      throws java.io.IOException {
    FetchItemImageRequest request =
        requirePayload(packet, FetchItemImageRequest.class, "Yêu cầu không hợp lệ.");

    Item item =
        itemService
            .getById(request.itemId())
            .orElseThrow(() -> new ValidationException("Sản phẩm không tồn tại."));

    String imagePath = item.getImageUrl();
    if (imagePath == null || imagePath.isBlank()) {
      throw new ValidationException("Sản phẩm không có ảnh.");
    }

    String base64Data = imageStorageService.readAsBase64(imagePath);
    sendSuccess(clientHandler, "OK", new FetchItemImageResponse(request.itemId(), base64Data));
    logger.info("Served image for itemId={}, path={}", request.itemId(), imagePath);
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.FETCH_ITEM_IMAGE;
  }

  @Override
  protected String ioErrorMessage() {
    return "Không thể đọc file ảnh trên server.";
  }
}
