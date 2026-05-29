package app.common.enums;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RequestTypeTest {
  @Test
  void publicRequests_allowGuestAccess() {
    assertTrue(RequestType.LOGIN.isAllowed(null));
    assertTrue(RequestType.REGISTER.isAllowed(null));
    assertTrue(RequestType.FETCH_AUCTION_SUMMARIES.isAllowed(null));
    assertTrue(RequestType.UNWATCH_AUCTION.isAllowed(null));
  }

  @Test
  void authenticatedRequests_rejectGuestAndAllowAnyRole() {
    assertFalse(RequestType.CHAT.isAllowed(null));
    assertFalse(RequestType.FETCH_AUCTION_HISTORY.isAllowed(null));
    assertFalse(RequestType.DEPOSIT.isAllowed(null));

    assertTrue(RequestType.CHAT.isAllowed(UserRole.BIDDER));
    assertTrue(RequestType.FETCH_AUCTION_HISTORY.isAllowed(UserRole.SELLER));
    assertTrue(RequestType.DEPOSIT.isAllowed(UserRole.ADMIN));
  }

  @Test
  void bidderRequests_allowOnlyBidder() {
    assertTrue(RequestType.PLACE_BID.isAllowed(UserRole.BIDDER));
    assertTrue(RequestType.SET_AUTO_BID.isAllowed(UserRole.BIDDER));
    assertTrue(RequestType.DISABLE_AUTO_BID.isAllowed(UserRole.BIDDER));
    assertFalse(RequestType.PLACE_BID.isAllowed(UserRole.SELLER));
    assertFalse(RequestType.SET_AUTO_BID.isAllowed(UserRole.SELLER));
    assertFalse(RequestType.DISABLE_AUTO_BID.isAllowed(UserRole.SELLER));
    assertFalse(RequestType.PLACE_BID.isAllowed(UserRole.ADMIN));
    assertFalse(RequestType.SET_AUTO_BID.isAllowed(UserRole.ADMIN));
    assertFalse(RequestType.DISABLE_AUTO_BID.isAllowed(UserRole.ADMIN));
  }

  @Test
  void sellerAndAdminRequests_rejectBidder() {
    assertFalse(RequestType.CREATE_AUCTION.isAllowed(UserRole.BIDDER));
    assertFalse(RequestType.UPDATE_AUCTION.isAllowed(UserRole.BIDDER));
    assertFalse(RequestType.CANCEL_AUCTION.isAllowed(UserRole.BIDDER));
    assertFalse(RequestType.FETCH_SELLER_ITEMS.isAllowed(UserRole.BIDDER));

    assertTrue(RequestType.CREATE_AUCTION.isAllowed(UserRole.SELLER));
    assertTrue(RequestType.UPDATE_AUCTION.isAllowed(UserRole.SELLER));
    assertTrue(RequestType.CANCEL_AUCTION.isAllowed(UserRole.SELLER));
    assertTrue(RequestType.FETCH_SELLER_ITEMS.isAllowed(UserRole.SELLER));

    assertFalse(RequestType.CREATE_AUCTION.isAllowed(UserRole.ADMIN));
    assertTrue(RequestType.UPDATE_AUCTION.isAllowed(UserRole.ADMIN));
    assertTrue(RequestType.CANCEL_AUCTION.isAllowed(UserRole.ADMIN));
    assertTrue(RequestType.FETCH_SELLER_ITEMS.isAllowed(UserRole.ADMIN));
  }

  @Test
  void adminRequests_allowOnlyAdmin() {
    assertTrue(RequestType.FETCH_USER_LIST.isAllowed(UserRole.ADMIN));
    assertTrue(RequestType.SETTLE_WALLET.isAllowed(UserRole.ADMIN));

    assertFalse(RequestType.FETCH_USER_LIST.isAllowed(UserRole.BIDDER));
    assertFalse(RequestType.SETTLE_WALLET.isAllowed(UserRole.BIDDER));
    assertFalse(RequestType.FETCH_USER_LIST.isAllowed(UserRole.SELLER));
    assertFalse(RequestType.SETTLE_WALLET.isAllowed(UserRole.SELLER));
  }
}
