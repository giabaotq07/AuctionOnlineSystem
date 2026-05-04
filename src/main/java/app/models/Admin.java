package app.models;

import app.enums.UserRole;

public class Admin extends User {
  public Admin(int id, String name, Account account, Wallet wallet) {
    super(id, name, account, wallet);
  }

  public Admin(String name, Account account, Wallet wallet) {
    super(name, account, wallet);
  }

  @Override
  public UserRole getRole() {
    return UserRole.ADMIN;
  }
}
