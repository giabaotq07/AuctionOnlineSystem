package app.common.enums;

import java.util.EnumSet;
import java.util.Set;

/** RequestType. */
public enum RequestType {
  LOGIN(Access.PUBLIC),
  REGISTER(Access.PUBLIC),
  PLACE_BID(UserRole.BIDDER),
  SET_AUTO_BID(UserRole.BIDDER),
  DISABLE_AUTO_BID(UserRole.BIDDER),
  CREATE_AUCTION(UserRole.SELLER, UserRole.ADMIN),
  UPDATE_AUCTION(UserRole.SELLER, UserRole.ADMIN),
  CHAT(Access.AUTHENTICATED),
  FETCH_AUCTION_SUMMARIES(Access.PUBLIC),
  FETCH_AUCTION_HISTORY(Access.AUTHENTICATED),
  FETCH_AUCTION_DETAIL(Access.AUTHENTICATED),
  UNWATCH_AUCTION(Access.PUBLIC),
  FETCH_SELLER_ITEMS(UserRole.SELLER, UserRole.ADMIN),
  FETCH_USER_LIST(UserRole.ADMIN),
  CANCEL_AUCTION(UserRole.SELLER, UserRole.ADMIN),
  DEPOSIT(Access.AUTHENTICATED),
  SETTLE_WALLET(UserRole.ADMIN),
  UPLOAD_IMAGE(UserRole.SELLER, UserRole.ADMIN),
  FETCH_ITEM_IMAGE(Access.PUBLIC);

  private final Access access;
  private final Set<UserRole> allowedRoles;

  RequestType(Access access) {
    this.access = access;
    this.allowedRoles = Set.of();
  }

  RequestType(UserRole firstRole, UserRole... additionalRoles) {
    this.access = Access.ROLES;
    this.allowedRoles = Set.copyOf(EnumSet.of(firstRole, additionalRoles));
  }

  /** Whether this request needs an authenticated session. */
  public boolean requiresAuthentication() {
    return access != Access.PUBLIC;
  }

  /** Whether a user role can execute this request. */
  public boolean isAllowed(UserRole role) {
    return switch (access) {
      case PUBLIC -> true;
      case AUTHENTICATED -> role != null;
      case ROLES -> role != null && allowedRoles.contains(role);
    };
  }

  private enum Access {
    PUBLIC,
    AUTHENTICATED,
    ROLES
  }
}
