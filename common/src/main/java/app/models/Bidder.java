package app.models;

import app.enums.UserRole;

/** Bidder. */
public class Bidder extends User {
  /** Bidder. */
  public Bidder(int id, String name, Account account, Wallet wallet) {
    super(id, name, account, wallet);
    role = UserRole.BIDDER;
  }

  /** Bidder. */
  public Bidder(String name, Account account, Wallet wallet) {
    super(name, account, wallet);
    role = UserRole.BIDDER;
  }

  @Override
  public UserRole getRole() {
    return UserRole.BIDDER;
  }
}
