package app.service;

import app.dao.AuctionDao;
import app.dao.ItemDao;
import app.database.TransactionManager;
import app.enums.AuctionStatus;
import app.enums.ItemStatus;
import app.enums.UserRole;
import app.exception.ServiceException;
import app.models.Item;
import java.util.List;
import java.util.Optional;

/** ItemService. */
public class ItemService {
  private final ItemDao itemDao;
  private final AuctionDao auctionDao;
  private final TransactionManager transactionManager;

  /** ItemService. */
  public ItemService(ItemDao itemDao, TransactionManager transactionManager) {
    this(itemDao, null, transactionManager);
  }

  /** ItemService. */
  public ItemService(
      ItemDao itemDao, AuctionDao auctionDao, TransactionManager transactionManager) {
    this.itemDao = itemDao;
    this.auctionDao = auctionDao;
    this.transactionManager = transactionManager;
  }

  /** add. */
  public Item add(Item item) {
    return transactionManager.runInTransaction(conn -> itemDao.save(conn, item));
  }

  /** getById. */
  public Optional<Item> getById(int id) {
    return itemDao.findById(id);
  }

  /** update. */
  public void update(Item item) {
    transactionManager.runInTransaction(
        conn -> {
          itemDao.update(conn, item);
          return null;
        });
  }

  /** updateStatus. */
  public void updateStatus(int id, ItemStatus status) {
    transactionManager.runInTransaction(
        conn -> {
          itemDao
              .findById(conn, id)
              .map(
                  item -> {
                    item.setStatus(status);
                    itemDao.update(conn, item);
                    return null;
                  });
          return null;
        });
  }

  /** delete. */
  public void delete(int id) {
    updateStatus(id, ItemStatus.DELETE);
  }

  /** getSellerItems. */
  public List<Item> getSellerItems(int requesterId, UserRole requesterRole, int requestedSellerId) {
    if (requesterRole != UserRole.ADMIN && requesterId != requestedSellerId) {
      throw new ServiceException("Bạn không có quyền xem danh sách sản phẩm này.");
    }
    return itemDao.findBySeller(requestedSellerId);
  }

  /** updateManagedItem. */
  public Item updateManagedItem(Item item, int requesterId, UserRole requesterRole) {
    validateManageRequest(item.getId(), requesterId, requesterRole);
    validateItemData(item);
    return transactionManager.runInTransaction(
        conn -> {
          Item stored =
              itemDao
                  .findById(conn, item.getId())
                  .orElseThrow(() -> new ServiceException("Không tìm thấy sản phẩm."));
          ensureManagePermission(stored, requesterId, requesterRole);
          ensureItemNotRunning(conn, stored.getId());
          stored.setName(item.getName());
          stored.setDescription(item.getDescription());
          stored.setStartingPrice(item.getStartingPrice());
          stored.setStepPrice(item.getStepPrice());
          stored.setType(item.getType());
          itemDao.update(conn, stored);
          return stored;
        });
  }

  /** softDeleteManagedItem. */
  public Item softDeleteManagedItem(int itemId, int requesterId, UserRole requesterRole) {
    validateManageRequest(itemId, requesterId, requesterRole);
    return transactionManager.runInTransaction(
        conn -> {
          Item stored =
              itemDao
                  .findById(conn, itemId)
                  .orElseThrow(() -> new ServiceException("Không tìm thấy sản phẩm."));
          ensureManagePermission(stored, requesterId, requesterRole);
          ensureItemNotRunning(conn, itemId);
          stored.setStatus(ItemStatus.DELETE);
          itemDao.update(conn, stored);
          return stored;
        });
  }

  public List<Item> getAll() {
    return itemDao.findAll();
  }

  private void validateManageRequest(int itemId, int requesterId, UserRole requesterRole) {
    if (itemId <= 0 || requesterId <= 0 || requesterRole == null) {
      throw new ServiceException("Dữ liệu sản phẩm không hợp lệ.");
    }
    if (requesterRole != UserRole.SELLER && requesterRole != UserRole.ADMIN) {
      throw new ServiceException("Bạn không có quyền quản lý sản phẩm.");
    }
  }

  private void validateItemData(Item item) {
    if (item == null
        || item.getName() == null
        || item.getName().isBlank()
        || item.getDescription() == null
        || item.getDescription().isBlank()
        || item.getStartingPrice() <= 0
        || item.getStepPrice() <= 0
        || item.getType() == null) {
      throw new ServiceException("Thông tin sản phẩm không hợp lệ.");
    }
  }

  private void ensureManagePermission(Item item, int requesterId, UserRole requesterRole) {
    if (requesterRole != UserRole.ADMIN && item.getSellerId() != requesterId) {
      throw new ServiceException("Bạn không có quyền quản lý sản phẩm này.");
    }
  }

  private void ensureItemNotRunning(java.sql.Connection conn, int itemId) {
    if (auctionDao == null) {
      return;
    }
    boolean running =
        auctionDao.findByItemId(conn, itemId).stream()
            .anyMatch(auction -> auction.getStatus() == AuctionStatus.RUNNING);
    if (running) {
      throw new ServiceException("Không thể sửa/xóa sản phẩm đang đấu giá.");
    }
  }
}
