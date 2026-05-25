package app.common.models;

import app.common.enums.UserRole;
import java.util.Objects;

/** User. */
public class User extends Entity {
  private String name;
  private Account account;
  private Wallet wallet;
  private String avatarUrl;
  private boolean status;


  /** User. */
  public User(int id, String name, Account account, Wallet wallet, String avatarUrl) {
    super(id);
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank.");
    }
    this.name = name;
    this.account = Objects.requireNonNull(account, "account");
    this.wallet = Objects.requireNonNull(wallet, "wallet");
    this.avatarUrl = avatarUrl;
    this.status = true;
  }

  /** User. */
  public User(int id, String name, Account account, Wallet wallet) {
    this(id, name, account, wallet, null);
  }

  /** User. */
  public User(String name, Account account, Wallet wallet) {
    this(0, name, account, wallet, null);
  }

  private User(
      int id,
      String name,
      Account account,
      Wallet wallet,
      String avatarUrl,
      boolean allowPublicView) {
    super(id);
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank.");
    }
    this.name = name;
    this.account = Objects.requireNonNull(account, "account");
    this.wallet = allowPublicView ? wallet : Objects.requireNonNull(wallet, "wallet");
    this.avatarUrl = avatarUrl;
    this.status = true;
  }

  /** Returns a user object safe to embed inside public auction data. */
  public User publicView() {
    return new User(
        getId(),
        name,
        new Account(account.getUsername(), null, account.getRole()),
        null,
        avatarUrl,
        true);
  }

  public static User createPublicUser(int id, String name, Account account, String avatarUrl) {
    return new User(id, name, account, null, avatarUrl, true);
  }

  public static User createPublicUser(int id, String name, Account account) {
    return createPublicUser(id, name, account, null);
  }

  public boolean isBanned() {
    return !status;
  }

  public void ban() {
    status = false;
  }

  public void unban() {
    status = true;
  }

  public void setStatus(boolean status) {
    this.status = status;
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

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  public boolean getStatus() {
    return status;
  }
}
