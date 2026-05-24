package app.server.service;

import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.models.Item;
import app.server.dao.AuctionDAO;
import app.server.dao.ItemDAO;
import app.server.database.TransactionManager;
import java.util.List;
import java.util.Optional;

/** ItemService. */
public class ItemService {
  private final ItemDAO itemDAO;
  private final AuctionDAO auctionDAO;
  private final TransactionManager transactionManager;

  /** ItemService. */
  public ItemService(
      ItemDAO itemDAO, AuctionDAO auctionDAO, TransactionManager transactionManager) {
    this.itemDAO = itemDAO;
    this.auctionDAO = auctionDAO;
    this.transactionManager = transactionManager;
  }

  /** add. */
  public Item add(Item item) {
    return transactionManager.runInTransaction(conn -> itemDAO.save(conn, item));
  }

  /** getById. */
  public Optional<Item> getById(int id) {
    return itemDAO.findById(id);
  }

  /** update. */
  public void update(Item item) {
    transactionManager.runInTransaction(
        conn -> {
          itemDAO.update(conn, item);
          return null;
        });
  }

  /** delete. */
  public void delete(int id) {
    transactionManager.runWithoutResult(
        conn ->
            itemDAO
                .findById(conn, id)
                .ifPresent(
                    item -> {
                      item.setDeleted(true);
                      itemDAO.update(conn, item);
                    }));
  }

  /** getSellerItems. */
  public List<Item> getSellerItems(int requesterId, UserRole requesterRole, int requestedSellerId) {
    OwnershipGuard.requireSellerOwnerOrAdmin(requesterId, requesterRole, requestedSellerId);

    return itemDAO.findBySeller(requestedSellerId);
  }

  /** updateManagedItem. */
  public Item updateManagedItem(Item item, int requesterId, UserRole requesterRole) {
    if (item == null || item.getId() <= 0) {
      throw new ServiceException("Dữ liệu sản phẩm không hợp lệ.");
    }

    return transactionManager.runInTransaction(
        conn -> {
          Item stored =
              itemDAO
                  .findById(conn, item.getId())
                  .orElseThrow(() -> new ServiceException("Không tìm thấy sản phẩm."));
          OwnershipGuard.requireSellerOwnerOrAdmin(
              requesterId, requesterRole, stored.getSellerId());

          stored.setName(item.getName());
          stored.setDescription(item.getDescription());
          stored.setStartingPrice(item.getStartingPrice());
          stored.setStepPrice(item.getStepPrice());
          stored.setType(item.getType());
          itemDAO.update(conn, stored);
          return stored;
        });
  }

  /** softDeleteManagedItem. */
  public Item softDeleteManagedItem(int itemId, int requesterId, UserRole requesterRole) {
    if (itemId <= 0) {
      throw new ServiceException("Sản phẩm không hợp lệ.");
    }

    return transactionManager.runInTransaction(
        conn -> {
          Item stored =
              itemDAO
                  .findById(conn, itemId)
                  .orElseThrow(() -> new ServiceException("Không tìm thấy sản phẩm."));
          OwnershipGuard.requireSellerOwnerOrAdmin(
              requesterId, requesterRole, stored.getSellerId());

          stored.setDeleted(true);
          itemDAO.update(conn, stored);
          return stored;
        });
  }

  public List<Item> getAll() {
    return itemDAO.findAll();
  }

  /**
   * Cập nhật đường dẫn ảnh cho item. Trả về relative path của ảnh cũ để caller có thể xoá file nếu
   * cần.
   */
  public String updateImagePath(
      int itemId, String imagePath, int requesterId, UserRole requesterRole) {
    if (itemId <= 0) {
      throw new ServiceException("Sản phẩm không hợp lệ.");
    }

    return transactionManager.runInTransaction(
        conn -> {
          Item stored =
              itemDAO
                  .findById(conn, itemId)
                  .orElseThrow(() -> new ServiceException("Không tìm thấy sản phẩm."));
          OwnershipGuard.requireSellerOwnerOrAdmin(
              requesterId, requesterRole, stored.getSellerId());

          String oldImagePath = stored.getImageUrl();
          stored.setImageUrl(imagePath);
          itemDAO.update(conn, stored);
          return oldImagePath;
        });
  }
}
