package app.models;

import app.enums.UserRole;

public class Seller extends User {
  public Seller(int id, String name, Account account, Wallet wallet) {
    super(id, name, account, wallet);
    role = UserRole.SELLER;
  }

  public Seller(String name, Account account, Wallet wallet) {
    super(name, account, wallet);
    role = UserRole.SELLER;
  }

  @Override
  public UserRole getRole() {
    return UserRole.SELLER;
  }
}
