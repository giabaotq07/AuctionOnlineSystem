package app.server.command;

import app.common.dto.FetchSellerItemsRequest;
import app.common.dto.ItemListResponse;
import app.common.enums.ResponseType;
import app.common.exception.ValidationException;
import app.common.mapper.ModelMapper;
import app.common.models.Item;
import app.common.models.User;
import app.common.protocol.PacketReq;
import app.server.network.ClientHandler;
import app.server.service.ItemService;
import java.util.List;

/** FetchSellerItemsCommand. */
public class FetchSellerItemsCommand extends SafeCommand {
  private final ItemService itemService;

  /** FetchSellerItemsCommand. */
  public FetchSellerItemsCommand(ItemService itemService) {
    this.itemService = itemService;
  }

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) {
    FetchSellerItemsRequest request =
        requirePayload(packet, FetchSellerItemsRequest.class, "Dữ liệu seller không hợp lệ.");
    if (request.sellerId() <= 0) {
      throw new ValidationException("Dữ liệu seller không hợp lệ.");
    }
    User user = requireUser(clientHandler);
    List<Item> items = itemService.getSellerItems(user.getId(), user.getRole(), request.sellerId());
    sendSuccess(
        clientHandler,
        "OK",
        new ItemListResponse(items.stream().map(ModelMapper::toItemPreview).toList()));
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.FETCH_SELLER_ITEMS_RESULT;
  }

  @Override
  protected String unexpectedErrorMessage() {
    return "Không thể tải danh sách sản phẩm.";
  }
}
