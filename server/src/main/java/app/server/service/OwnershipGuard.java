package app.server.service;

import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.models.Auction;
import app.common.models.User;

/** Domain ownership checks that cannot be decided from RequestType alone. */
public final class OwnershipGuard {
  private static final String FORBIDDEN = "Bạn không có quyền thực hiện yêu cầu này.";

  private OwnershipGuard() {}

  public static void requireValidActor(User actor) {
    if (actor == null || actor.getId() <= 0) {
      throw new ServiceException("Người dùng không hợp lệ.");
    }
  }

  public static void requireAuctionOwnerOrAdmin(Auction auction, User actor) {
    requireValidActor(actor);
    if (actor.getRole() == UserRole.ADMIN) {
      return;
    }
    if (auction == null || auction.getSellerId() != actor.getId()) {
      throw new ServiceException(FORBIDDEN);
    }
  }

  public static void requireNotAuctionSeller(Auction auction, User actor, String deniedMessage) {
    requireValidActor(actor);
    if (auction != null && auction.getSellerId() == actor.getId()) {
      throw new ServiceException(deniedMessage);
    }
  }

  public static void requireSellerOwnerOrAdmin(
      int requesterId, UserRole requesterRole, int sellerId) {
    if (requesterId <= 0 || sellerId <= 0 || requesterRole == null) {
      throw new ServiceException("Dữ liệu quyền truy cập không hợp lệ.");
    }
    if (requesterRole == UserRole.ADMIN) {
      return;
    }
    if (requesterId != sellerId) {
      throw new ServiceException(FORBIDDEN);
    }
  }
}
