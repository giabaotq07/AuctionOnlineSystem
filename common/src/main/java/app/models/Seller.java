package app.models;

import app.enums.UserRole;

/** Seller. */
public class Seller extends User {
  /** Seller. */
  public Seller(int id, String name, Account account, Wallet wallet) {
    super(id, name, account, wallet);
    role = UserRole.SELLER;
  }

  /** Seller. */
  public Seller(String name, Account account, Wallet wallet) {
    super(name, account, wallet);
    role = UserRole.SELLER;
  }

  @Override
  public UserRole getRole() {
    return UserRole.SELLER;
  }
}
