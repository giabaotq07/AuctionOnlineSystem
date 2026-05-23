package app.server.network;

import static org.junit.jupiter.api.Assertions.*;

import app.common.models.User;
import org.junit.jupiter.api.Test;

/** SessionTest. Kiem thu trang thai phien lam viec (Session) tren server. */
public class SessionTest {

  @Test
  public void testSessionLifecycle() {
    Session session = new Session();
    assertNotNull(session.getSessionId());
    assertNotNull(session.getCreatedAt());
    assertNotNull(session.getLastAccessTime());
    assertFalse(session.isAuthenticated());
    assertNull(session.getUser());
    assertNull(session.getViewingAuctionId());

    // Authenticate
    User user = app.TestFixtures.user("testuser", app.common.enums.UserRole.BIDDER);
    user.setId(10);
    session.authenticate(user);

    assertTrue(session.isAuthenticated());
    assertEquals(user, session.getUser());

    // Viewing auction
    session.setViewingAuctionId(5);
    assertEquals(5, session.getViewingAuctionId());

    // Logout
    session.logout();
    assertFalse(session.isAuthenticated());
    assertNull(session.getUser());
    assertNull(session.getViewingAuctionId());
  }
}
