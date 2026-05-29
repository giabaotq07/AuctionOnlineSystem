package app.client.manager;

import app.common.models.User;

public class UserManager {
  private static volatile UserManager instance;
  private User currentUser;
  private final java.util.Map<Integer, String> avatarBase64Cache =
      new java.util.concurrent.ConcurrentHashMap<>();
  private final java.util.Map<Integer, String> avatarUrlCache =
      new java.util.concurrent.ConcurrentHashMap<>();

  private UserManager() {}

  public void setAvatarBase64(int userId, String base64Data) {
    setAvatarBase64(userId, null, base64Data);
  }

  public void setAvatarBase64(int userId, String avatarUrl, String base64Data) {
    if (base64Data != null && !base64Data.isBlank()) {
      this.avatarBase64Cache.put(userId, base64Data);
      if (avatarUrl != null && !avatarUrl.isBlank()) {
        this.avatarUrlCache.put(userId, avatarUrl);
      } else {
        this.avatarUrlCache.remove(userId);
      }
    }
  }

  public java.util.Optional<String> getAvatarBase64(int userId) {
    return java.util.Optional.ofNullable(this.avatarBase64Cache.get(userId));
  }

  public java.util.Optional<String> getAvatarBase64(int userId, String avatarUrl) {
    String cachedBase64 = this.avatarBase64Cache.get(userId);
    if (cachedBase64 == null) {
      return java.util.Optional.empty();
    }
    if (avatarUrl != null && !avatarUrl.isBlank()) {
      String cachedAvatarUrl = this.avatarUrlCache.get(userId);
      if (cachedAvatarUrl != null && !cachedAvatarUrl.equals(avatarUrl)) {
        return java.util.Optional.empty();
      }
    }
    return java.util.Optional.of(cachedBase64);
  }

  public void clearAvatarBase64(int userId) {
    this.avatarBase64Cache.remove(userId);
    this.avatarUrlCache.remove(userId);
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
