package app.server.service;

import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.models.Item;
import app.server.dao.ItemDAO;
import java.util.List;

/** ItemService. */
public class ItemService {
  private final ItemDAO itemDAO;

  /** ItemService. */
  public ItemService(ItemDAO itemDAO) {
    this.itemDAO = itemDAO;
  }

  /** getSellerItems. */
  public List<Item> getSellerItems(int requesterId, UserRole requesterRole, int requestedSellerId) {
    if (requesterRole != UserRole.ADMIN && requesterId != requestedSellerId) {
      throw new ServiceException("Bạn không có quyền xem danh sách sản phẩm này.");
    }
    return itemDAO.findBySeller(requestedSellerId);
  }
}
