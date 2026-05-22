package app.common.models;

import app.common.enums.UserRole;

/** UserFactory. */
public class UserFactory {
  /** createUser. */
  public static User createUser(String name, Account account, Wallet wallet, UserRole role) {
    account.setRole(role);
    return new User(name, account, wallet);
  }

  /** createUser. */
  public static User createUser(
      int id, String name, Account account, Wallet wallet, UserRole role) {
    account.setRole(role);
    return new User(id, name, account, wallet);
  }
}
