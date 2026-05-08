package app.models;

import app.enums.UserRole;

public class Admin extends User {
  public Admin(int id, String name, Account account, Wallet wallet) {
    super(id, name, account, wallet);
    role = UserRole.ADMIN;
  }

  public Admin(String name, Account account, Wallet wallet) {
    super(name, account, wallet);
    role = UserRole.ADMIN;
  }

  @Override
  public UserRole getRole() {
    return UserRole.ADMIN;
  }
}
