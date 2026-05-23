// server/src/main/java/app/server/command/FetchItemImageCommand.java
package app.server.command;

import app.common.dto.FetchItemImageRequest;
import app.common.dto.FetchItemImageResponse;
import app.common.enums.ResponseType;
import app.common.exception.ValidationException;
import app.common.protocol.PacketReq;
import app.server.network.ClientHandler;
import app.server.service.ImageStorageService;

/** Đọc file ảnh từ server_data, encode Base64, gửi về client qua Socket. */
public class FetchItemImageCommand extends SafeCommand {
  private final ImageStorageService imageStorageService;

  public FetchItemImageCommand(ImageStorageService imageStorageService) {
    this.imageStorageService = imageStorageService;
  }

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet)
      throws java.io.IOException {
    FetchItemImageRequest request =
        requirePayload(packet, FetchItemImageRequest.class, "Đường dẫn ảnh không hợp lệ.");
    if (request.imagePath() == null || request.imagePath().isBlank()) {
      throw new ValidationException("Đường dẫn ảnh không hợp lệ.");
    }
    String base64Data = imageStorageService.readAsBase64(request.imagePath());
    sendSuccess(clientHandler, "OK", new FetchItemImageResponse(request.itemId(), base64Data));
    logger.info("Served image for itemId={}, path={}", request.itemId(), request.imagePath());
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
