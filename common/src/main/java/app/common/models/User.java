package app.common.models;

import app.common.enums.UserRole;
import java.util.Objects;

/** User. */
public class User extends Entity {
  protected String name;
  protected Account account;
  protected Wallet wallet;

  /** User. */
  public User(int id, String name, Account account, Wallet wallet) {
    super(id);
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank.");
    }
    this.name = name;
    this.account = Objects.requireNonNull(account, "account");
    this.wallet = Objects.requireNonNull(wallet, "wallet");
  }

  /** User. */
  public User(String name, Account account, Wallet wallet) {
    super();
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank.");
    }
    this.name = name;
    this.account = Objects.requireNonNull(account, "account");
    this.wallet = Objects.requireNonNull(wallet, "wallet");
  }

  private User(int id, String name, Account account, Wallet wallet, boolean allowPublicView) {
    super(id);
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank.");
    }
    this.name = name;
    this.account = Objects.requireNonNull(account, "account");
    this.wallet = allowPublicView ? wallet : Objects.requireNonNull(wallet, "wallet");
  }

  /** Returns a user object safe to embed inside public auction data. */
  public User publicView() {
    return new User(
        getId(), name, new Account(account.getUsername(), null, account.getRole()), null, true);
  }

  public static User createPublicUser(int id, String name, Account account) {
    return new User(id, name, account, null, true);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Account getAccount() {
    return account;
  }

  public Wallet getWallet() {
    return wallet;
  }

  /** getRole. */
  public UserRole getRole() {
    return account.getRole();
  }
}
