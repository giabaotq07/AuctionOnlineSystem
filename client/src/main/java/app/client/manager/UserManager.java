package app.client.manager;

import app.common.models.User;

public class UserManager {
  private static volatile UserManager instance;
  private User currentUser;
  private final java.util.Map<Integer, String> avatarBase64Cache =
      new java.util.concurrent.ConcurrentHashMap<>();

  private UserManager() {}

  public void setAvatarBase64(int userId, String base64Data) {
    if (base64Data != null && !base64Data.isBlank()) {
      this.avatarBase64Cache.put(userId, base64Data);
    }
  }

  public java.util.Optional<String> getAvatarBase64(int userId) {
    return java.util.Optional.ofNullable(this.avatarBase64Cache.get(userId));
  }

  public static UserManager getInstance() {
    if (instance == null) {
      synchronized (UserManager.class) {
        if (instance == null) {
          instance = new UserManager();
        }
      }
    }
    return instance;
  }

  public void setCurrentUser(User currentUser) {
    this.currentUser = currentUser;
  }

  public User getCurrentUser() {
    return currentUser;
  }
}
