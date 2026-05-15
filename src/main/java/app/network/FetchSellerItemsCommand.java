package app.network;

import app.dto.FetchSellerItemsRequest;
import app.dto.ItemData;
import app.dto.ItemListResponse;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.User;
import app.service.ItemService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FetchSellerItemsCommand. */
public class FetchSellerItemsCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(FetchSellerItemsCommand.class);
  private final ItemService itemService;

  /** FetchSellerItemsCommand. */
  public FetchSellerItemsCommand(ItemService itemService) {
    this.itemService = itemService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      FetchSellerItemsRequest request = packet.getData(FetchSellerItemsRequest.class);
      if (request == null || request.sellerId() <= 0) {
        sendError(clientHandler, "Dữ liệu seller không hợp lệ.");
        return;
      }
      User user = clientHandler.getUser();
      List<ItemData> items =
          itemService.getSellerItems(user.getId(), user.getRole(), request.sellerId()).stream()
              .map(ItemData::new)
              .toList();
      clientHandler.sendPacket(
          PacketRes.of(PacketType.FETCH_SELLER_ITEMS, new ItemListResponse(true, "OK", items)));
    } catch (ServiceException e) {
      logger.warn("Fetch seller items failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected fetch seller items error", e);
      sendError(clientHandler, "Không thể tải danh sách sản phẩm.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(
        PacketRes.of(
            false, PacketType.FETCH_SELLER_ITEMS, new ItemListResponse(false, message, List.of())));
  }
}
