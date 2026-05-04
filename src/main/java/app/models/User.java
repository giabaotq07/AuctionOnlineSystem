package app.models;

import app.enums.UserRole;
import java.time.LocalDateTime;

public abstract class User extends Entity {
  protected String name;
  protected final Account account;
  protected final Wallet wallet;
  protected LocalDateTime createdAt;
  protected LocalDateTime updatedAt;

  public User(int id, String name, Account account, Wallet wallet) {
    this.id = id;
    this.name = name;
    this.account = account;
    this.wallet = wallet;
  }

  public User(String name, Account account, Wallet wallet) {
    this.name = name;
    this.account = account;
    this.wallet = wallet;
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

  public abstract UserRole getRole();
}
