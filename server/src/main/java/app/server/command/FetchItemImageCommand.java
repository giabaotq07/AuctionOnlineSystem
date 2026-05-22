// server/src/main/java/app/server/command/FetchItemImageCommand.java
package app.server.command;

import app.common.dto.FetchItemImageRequest;
import app.common.dto.FetchItemImageResponse;
import app.common.enums.PacketType;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.service.ImageStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Đọc file ảnh từ server_data, encode Base64, gửi về client qua Socket. */
public class FetchItemImageCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(FetchItemImageCommand.class);

  private final ImageStorageService imageStorageService;

  public FetchItemImageCommand(ImageStorageService imageStorageService) {
    this.imageStorageService = imageStorageService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      FetchItemImageRequest request = packet.getData(FetchItemImageRequest.class);
      if (request == null || request.imagePath() == null || request.imagePath().isBlank()) {
        clientHandler.sendPacket(
            PacketRes.error(PacketType.FETCH_ITEM_IMAGE, "Đường dẫn ảnh không hợp lệ."));
        return;
      }
      // Server đọc file ảnh và encode sang Base64 — không để client tự đọc filesystem
      String base64Data = imageStorageService.readAsBase64(request.imagePath());

      clientHandler.sendPacket(
          PacketRes.of(
              PacketType.FETCH_ITEM_IMAGE,
              "OK",
              new FetchItemImageResponse(request.itemId(), base64Data)));

      logger.info("Served image for itemId={}, path={}", request.itemId(), request.imagePath());

    } catch (IllegalArgumentException e) {
      logger.warn("Invalid image request: {}", e.getMessage());
      clientHandler.sendPacket(PacketRes.error(PacketType.FETCH_ITEM_IMAGE, e.getMessage()));
    } catch (java.io.IOException e) {
      logger.error("Cannot read image file", e);
      clientHandler.sendPacket(
          PacketRes.error(PacketType.FETCH_ITEM_IMAGE, "Không thể đọc file ảnh trên server."));
    }
  }
}
