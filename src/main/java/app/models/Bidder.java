package app.models;

import app.enums.UserRole;

public class Bidder extends User {
  public Bidder(int id, String name, Account account, Wallet wallet) {
    super(id, name, account, wallet);
    role = UserRole.BIDDER;
  }

  public Bidder(String name, Account account, Wallet wallet) {
    super(name, account, wallet);
    role = UserRole.BIDDER;
  }

  @Override
  public UserRole getRole() {
    return UserRole.BIDDER;
  }
}
