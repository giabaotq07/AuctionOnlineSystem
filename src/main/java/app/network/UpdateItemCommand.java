package app.network;

import app.dto.ItemData;
import app.dto.ItemResponse;
import app.dto.UpdateItemRequest;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.models.Item;
import app.models.ItemFactory;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.User;
import app.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** UpdateItemCommand. */
public class UpdateItemCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(UpdateItemCommand.class);
  private final ItemService itemService;

  /** UpdateItemCommand. */
  public UpdateItemCommand(ItemService itemService) {
    this.itemService = itemService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      UpdateItemRequest request = packet.getData(UpdateItemRequest.class);
      if (request == null) {
        sendError(clientHandler, "Dữ liệu sản phẩm không hợp lệ.");
        return;
      }
      User user = clientHandler.getUser();
      Item item =
          ItemFactory.createItem(
              request.itemId(),
              request.name(),
              user.getId(),
              request.description(),
              request.startingPrice(),
              request.stepPrice(),
              request.type());
      Item updated = itemService.updateManagedItem(item, user.getId(), user.getRole());
      clientHandler.sendPacket(
          PacketRes.of(
              PacketType.UPDATE_ITEM,
              new ItemResponse(true, "Cập nhật sản phẩm thành công.", new ItemData(updated))));
    } catch (ServiceException e) {
      logger.warn("Update item failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected update item error", e);
      sendError(clientHandler, "Không thể cập nhật sản phẩm.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(
        PacketRes.of(false, PacketType.UPDATE_ITEM, new ItemResponse(false, message, null)));
  }
}
