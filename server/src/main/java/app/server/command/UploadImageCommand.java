package app.server.command;

import app.common.dto.AuctionDetailResponse;
import app.common.dto.UploadImageRequest;
import app.common.dto.UploadImageResponse;
import app.common.enums.PacketType;
import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.mapper.DtoMapper;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.AuctionService;
import app.server.service.AuctionSnapshot;
import app.server.service.ImageStorageService;
import app.server.service.ItemService;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Nhận ảnh Base64 từ client, lưu file, cập nhật DB. */
public class UploadImageCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(UploadImageCommand.class);

  private final ItemService itemService;
  private final ImageStorageService imageStorageService;
  private final AuctionService auctionService;

  /** Constructor. */
  public UploadImageCommand(
      ItemService itemService,
      ImageStorageService imageStorageService,
      AuctionService auctionService) {
    this.itemService = itemService;
    this.imageStorageService = imageStorageService;
    this.auctionService = auctionService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      UploadImageRequest request = packet.getData(UploadImageRequest.class);
      if (request == null || request.base64Data() == null || request.originalFileName() == null) {
        sendError(clientHandler, "Dữ liệu ảnh không hợp lệ.");
        return;
      }

      var user = clientHandler.getUser();
      UserRole role = user.getRole();

      // 1. Ghi file ra đĩa, lấy relative path
      String newImagePath =
          imageStorageService.save(request.base64Data(), request.originalFileName());

      // 2. Cập nhật DB; đồng thời lấy lại path ảnh cũ để xoá
      String oldImagePath =
          itemService.updateImagePath(request.itemId(), newImagePath, user.getId(), role);

      // 3. Xoá file ảnh cũ (nếu có) — không block response
      if (oldImagePath != null && !oldImagePath.isBlank()) {
        imageStorageService.deleteIfExists(oldImagePath);
      }

      clientHandler.sendPacket(
          PacketRes.of(
              PacketType.UPLOAD_IMAGE,
              "Tải ảnh thành công.",
              new UploadImageResponse(request.itemId(), newImagePath)));

      // 4. Invalidate cache & broadcast để tất cả client khác biết ảnh đã cập nhật
      auctionService.invalidateCache();
      Server.broadcastAuctionList(auctionService);

      // Broadcast AUCTION_DETAIL_UPDATED cho các client để đồng bộ chi tiết và tải ảnh mới
      for (AuctionSnapshot snapshot : auctionService.getAuctions()) {
        if (snapshot.item() != null && snapshot.item().getId() == request.itemId()) {
          AuctionDetailResponse detailResponse =
              new AuctionDetailResponse(
                  DtoMapper.toAuctionDetail(snapshot.auction(), snapshot.item()));
          Server.broadcast(
              PacketRes.of(PacketType.AUCTION_DETAIL_UPDATED, "OK", detailResponse), -1);
        }
      }

      logger.info("Image uploaded for item {} by user {}", request.itemId(), user.getId());

    } catch (ServiceException e) {
      logger.warn("Upload image service error: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (IllegalArgumentException e) {
      logger.warn("Upload image validation error: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (IOException e) {
      logger.error("Upload image IO error", e);
      sendError(clientHandler, "Không thể lưu file ảnh trên server.");
    } catch (Exception e) {
      logger.error("Unexpected upload image error", e);
      sendError(clientHandler, "Lỗi không xác định khi tải ảnh.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(PacketType.UPLOAD_IMAGE, message));
  }
}
