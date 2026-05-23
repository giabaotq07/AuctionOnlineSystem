package app.client.manager;

import static org.junit.jupiter.api.Assertions.*;

import app.common.enums.UserRole;
import app.common.models.Account;
import app.common.models.User;
import app.common.models.Wallet;
import org.junit.jupiter.api.Test;

/**
 * Lop kiem thu cho UserManager phia Client. Viet bang tieng Viet khong dau de mentor de dang giai
 * thich.
 */
public class UserManagerTest {

  /** Kiem thu tinh nang luu giu thong tin phien nguoi dung hien tai. */
  @Test
  public void testUserManagerSession() {
    UserManager manager = UserManager.getInstance();
    assertNotNull(manager);

    // Kiem tra singleton
    assertEquals(manager, UserManager.getInstance());

    // Mac dinh ban dau nguoi dung hien tai phai la null
    manager.setCurrentUser(null);
    assertNull(manager.getCurrentUser());

    // Set nguoi dung dang nhap hien tai
    User user =
        new User(55, "Tuong Mentor", new Account("mentor55", "pwd", UserRole.ADMIN), new Wallet());
    manager.setCurrentUser(user);

    assertNotNull(manager.getCurrentUser());
    assertEquals(55, manager.getCurrentUser().getId());
    assertEquals("Tuong Mentor", manager.getCurrentUser().getName());
    assertEquals(UserRole.ADMIN, manager.getCurrentUser().getAccount().getRole());

    // Xoa nguoi dung khi logout
    manager.setCurrentUser(null);
    assertNull(manager.getCurrentUser());
  }
}
