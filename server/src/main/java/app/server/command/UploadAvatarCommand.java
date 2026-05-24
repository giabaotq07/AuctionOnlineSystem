package app.server.command;

import app.common.dto.UploadAvatarRequest;
import app.common.dto.UploadAvatarResponse;
import app.common.enums.ResponseType;
import app.common.exception.ServiceException;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.service.ImageStorageService;
import app.server.service.UserService;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Nhận ảnh avatar Base64 từ client, lưu file, cập nhật DB của user. */
public class UploadAvatarCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(UploadAvatarCommand.class);

  private final UserService userService;
  private final ImageStorageService imageStorageService;

  public UploadAvatarCommand(UserService userService, ImageStorageService imageStorageService) {
    this.userService = userService;
    this.imageStorageService = imageStorageService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      UploadAvatarRequest request = packet.getData(UploadAvatarRequest.class);
      if (request == null || request.base64Data() == null || request.originalFileName() == null) {
        sendError(clientHandler, "Dữ liệu ảnh không hợp lệ.");
        return;
      }

      var user = clientHandler.getUser();
      if (user == null) {
        sendError(clientHandler, "Bạn chưa đăng nhập.");
        return;
      }

      // 1. Ghi file ra đĩa, lấy relative path
      String newAvatarUrl =
          imageStorageService.save(request.base64Data(), request.originalFileName());

      // 2. Cập nhật DB và lấy lại path ảnh cũ để xoá
      String oldAvatarUrl = userService.updateAvatarUrl(user.getId(), newAvatarUrl);

      // 3. Cập nhật reference user trong session
      user.setAvatarUrl(newAvatarUrl);

      // 4. Xoá file ảnh cũ (nếu có)
      if (oldAvatarUrl != null && !oldAvatarUrl.isBlank()) {
        imageStorageService.deleteIfExists(oldAvatarUrl);
      }

      clientHandler.sendPacket(
          PacketRes.of(
              ResponseType.UPLOAD_AVATAR,
              "Tải avatar thành công.",
              new UploadAvatarResponse(newAvatarUrl)));

      logger.info("Avatar updated for user {}", user.getId());

    } catch (ServiceException e) {
      logger.warn("Upload avatar service error: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (IllegalArgumentException e) {
      logger.warn("Upload avatar validation error: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (IOException e) {
      logger.error("Upload avatar IO error", e);
      sendError(clientHandler, "Không thể lưu file avatar trên server.");
    } catch (Exception e) {
      logger.error("Unexpected upload avatar error", e);
      sendError(clientHandler, "Lỗi không xác định khi tải avatar.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(ResponseType.UPLOAD_AVATAR, message));
  }
}
