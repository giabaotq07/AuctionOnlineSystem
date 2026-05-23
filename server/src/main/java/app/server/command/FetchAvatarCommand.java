package app.server.command;

import app.common.dto.FetchAvatarRequest;
import app.common.dto.FetchAvatarResponse;
import app.common.enums.ResponseType;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.service.ImageStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Đọc file avatar từ server_data, encode Base64, gửi về client qua Socket. */
public class FetchAvatarCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(FetchAvatarCommand.class);

  private final ImageStorageService imageStorageService;

  public FetchAvatarCommand(ImageStorageService imageStorageService) {
    this.imageStorageService = imageStorageService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      FetchAvatarRequest request = packet.getData(FetchAvatarRequest.class);
      if (request == null || request.avatarUrl() == null || request.avatarUrl().isBlank()) {
        clientHandler.sendPacket(
            PacketRes.error(ResponseType.FETCH_AVATAR, "Đường dẫn avatar không hợp lệ."));
        return;
      }

      // Server đọc file ảnh và encode sang Base64
      String base64Data = imageStorageService.readAsBase64(request.avatarUrl());

      clientHandler.sendPacket(
          PacketRes.of(
              ResponseType.FETCH_AVATAR,
              "OK",
              new FetchAvatarResponse(request.userId(), base64Data)));

      logger.info("Served avatar for userId={}, path={}", request.userId(), request.avatarUrl());

    } catch (IllegalArgumentException e) {
      logger.warn("Invalid avatar request: {}", e.getMessage());
      clientHandler.sendPacket(PacketRes.error(ResponseType.FETCH_AVATAR, e.getMessage()));
    } catch (java.io.IOException e) {
      logger.error("Cannot read avatar file", e);
      clientHandler.sendPacket(
          PacketRes.error(ResponseType.FETCH_AVATAR, "Không thể đọc file avatar trên server."));
    }
  }
}
