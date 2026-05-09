package app.models;

import app.data.UserData;
import app.enums.UserRole;

public class UserFactory implements java.io.Serializable {
  public static User createUser(String name, Account account, Wallet wallet, UserRole role) {
    return switch (role) {
      case ADMIN -> new Admin(name, account, wallet);
      case SELLER -> new Seller(name, account, wallet);
      case BIDDER -> new Bidder(name, account, wallet);
    };
  }

  public static User createUser(
      int id, String name, Account account, Wallet wallet, UserRole role) {
    return switch (role) {
      case ADMIN -> new Admin(id, name, account, wallet);
      case SELLER -> new Seller(id, name, account, wallet);
      case BIDDER -> new Bidder(id, name, account, wallet);
    };
  }

  public static User createUser(UserData userData) {
    UserRole role = userData.role();
    return switch (role) {
      case ADMIN ->
          new Admin(
              userData.id(),
              userData.name(),
              new Account(userData.username(), null),
              new Wallet(userData.assets()));
      case SELLER ->
          new Seller(
              userData.id(),
              userData.name(),
              new Account(userData.username(), null),
              new Wallet(userData.assets()));
      case BIDDER ->
          new Bidder(
              userData.id(),
              userData.name(),
              new Account(userData.username(), null),
              new Wallet(userData.assets()));
    };
  }
}
