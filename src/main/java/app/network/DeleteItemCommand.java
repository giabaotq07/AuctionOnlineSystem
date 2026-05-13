package app.network;

import app.data.DeleteItemRequest;
import app.data.ItemData;
import app.data.ItemResponse;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.models.Item;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.User;
import app.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteItemCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(DeleteItemCommand.class);
  private final ItemService itemService;

  public DeleteItemCommand(ItemService itemService) {
    this.itemService = itemService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      DeleteItemRequest request = packet.getData(DeleteItemRequest.class);
      if (request == null || request.itemId() <= 0) {
        sendError(clientHandler, "Dữ liệu sản phẩm không hợp lệ.");
        return;
      }
      User user = clientHandler.getUser();
      Item deleted =
          itemService.softDeleteManagedItem(request.itemId(), user.getId(), user.getRole());
      clientHandler.sendPacket(
          PacketRes.of(
              PacketType.DELETE_ITEM,
              new ItemResponse(true, "Xóa sản phẩm thành công.", new ItemData(deleted))));
    } catch (ServiceException e) {
      logger.warn("Delete item failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected delete item error", e);
      sendError(clientHandler, "Không thể xóa sản phẩm.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(
        PacketRes.of(false, PacketType.DELETE_ITEM, new ItemResponse(false, message, null)));
  }
}
