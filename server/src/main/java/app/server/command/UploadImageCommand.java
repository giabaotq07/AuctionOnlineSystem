package app.server.command;

import app.common.dto.AuctionDetailResponse;
import app.common.dto.UploadImageRequest;
import app.common.dto.UploadImageResponse;
import app.common.enums.ResponseType;
import app.common.enums.UserRole;
import app.common.exception.ValidationException;
import app.common.models.Auction;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.AuctionQueryService;
import app.server.service.ImageStorageService;
import app.server.service.ItemService;

/** Nhận ảnh Base64 từ client, lưu file, cập nhật DB. */
public class UploadImageCommand extends SafeCommand {
  private final ItemService itemService;
  private final ImageStorageService imageStorageService;
  private final AuctionQueryService auctionQueryService;

  /** Constructor. */
  public UploadImageCommand(
      ItemService itemService,
      ImageStorageService imageStorageService,
      AuctionQueryService auctionQueryService) {
    this.itemService = itemService;
    this.imageStorageService = imageStorageService;
    this.auctionQueryService = auctionQueryService;
  }

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) throws Exception {
    String newImagePath = null;
    try {
      UploadImageRequest request =
          requirePayload(packet, UploadImageRequest.class, "Dữ liệu ảnh không hợp lệ.");
      if (request.itemId() <= 0
          || request.base64Data() == null
          || request.originalFileName() == null) {
        throw new ValidationException("Dữ liệu ảnh không hợp lệ.");
      }

      var user = requireUser(clientHandler);
      UserRole role = user.getRole();

      // 1. Ghi file ra đĩa, lấy relative path
      newImagePath = imageStorageService.save(request.base64Data(), request.originalFileName());

      // 2. Cập nhật DB; đồng thời lấy lại path ảnh cũ để xoá
      String oldImagePath =
          itemService.updateImagePath(request.itemId(), newImagePath, user.getId(), role);

      // 3. Xoá file ảnh cũ (nếu có) — không block response
      if (oldImagePath != null && !oldImagePath.isBlank()) {
        imageStorageService.deleteIfExists(oldImagePath);
      }

      clientHandler.sendPacket(
          PacketRes.of(
              ResponseType.UPLOAD_IMAGE,
              "Tải ảnh thành công.",
              new UploadImageResponse(request.itemId(), newImagePath)));

      notifyAfterUpload(request.itemId());

      logger.info("Image uploaded for item {} by user {}", request.itemId(), user.getId());

    } catch (Exception e) {
      imageStorageService.deleteIfExists(newImagePath);
      throw e;
    }
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.UPLOAD_IMAGE;
  }

  @Override
  protected String ioErrorMessage() {
    return "Không thể lưu file ảnh trên server.";
  }

  @Override
  protected String unexpectedErrorMessage() {
    return "Lỗi không xác định khi tải ảnh.";
  }

  private void notifyAfterUpload(int itemId) {
    try {
      Server.broadcastAuctionList(auctionQueryService);
      for (Auction auction : auctionQueryService.getAuctionsByItem(itemId)) {
        AuctionDetailResponse detailResponse =
            new AuctionDetailResponse(
                app.common.mapper.ModelMapper.toAuctionDto(
                    auctionQueryService.getAuctionDetail(auction.getId())));
        Server.broadcast(
            PacketRes.of(ResponseType.AUCTION_DETAIL_UPDATED, "OK", detailResponse), -1);
      }
    } catch (Exception e) {
      logger.warn("Image for item {} uploaded, but notification failed", itemId, e);
    }
  }
}
