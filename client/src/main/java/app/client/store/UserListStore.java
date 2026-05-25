package app.client.store;

import app.common.dto.UserDto;
import java.util.ArrayList;
import java.util.List;

public final class UserListStore {
  private final List<UserDto> masterUsers = new ArrayList<>();

  private static volatile UserListStore instance;

  private UserListStore() {}

  public static UserListStore getInstance() {
    if (instance == null) {
      synchronized (UserListStore.class) {
        if (instance == null) {
          instance = new UserListStore();
        }
      }
    }
    return instance;
  }

  public synchronized List<UserDto> getMasterUsers() {
    return new ArrayList<>(masterUsers);
  }

  public synchronized void setMasterUsers(List<UserDto> users) {
    masterUsers.clear();
    if (users != null) {
      masterUsers.addAll(users);
    }
  }

  public synchronized void updateUser(UserDto updatedUser) {
    if (updatedUser == null) {
      return;
    }
    for (int i = 0; i < masterUsers.size(); i++) {
      if (masterUsers.get(i).id() == updatedUser.id()) {
        masterUsers.set(i, updatedUser);
        break;
      }
    }
  }

  public synchronized void updateUserBannedStatus(int userId, boolean isBanned) {
    for (int i = 0; i < masterUsers.size(); i++) {
      if (masterUsers.get(i).id() == userId) {
        UserDto user = masterUsers.get(i);
        masterUsers.set(
            i,
            new UserDto(
                user.id(), user.name(), user.account(), user.wallet(), user.avatarUrl(), isBanned));
        break;
      }
    }
  }

  public synchronized void clear() {
    masterUsers.clear();
  }
}
