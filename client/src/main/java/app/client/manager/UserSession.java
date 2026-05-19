package app.client.manager;

import app.common.models.User;

public class UserSession {
  private static volatile UserSession instance;
  private User currentUser;
  private UserSession() {}
  public static UserSession getInstance() {
    if (instance == null) {
      synchronized (UserSession.class) {
        if (instance == null) {
          instance = new UserSession();
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
