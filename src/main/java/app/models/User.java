package app.models;

import java.util.Objects;

public class User extends Entity {
  private int id;
  private String name;
  private String account;
  private double assets;
  private String password;

  public User(String name, String account, String password) {
    this.account = account;
    this.password = password;
    this.assets = 0;
    this.name = name;
  }
  public User(int id, String name, String account, String password) {
    this.id = id;
    this.account = account;
    this.password = password;
    this.assets = 0;
    this.name = name;
  }

  @Override
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getAccount() {
    return account;
  }

  public double getAssets() {
    return assets;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setAccount(String account) {
    this.account = account;
  }



  public void Deposit(double amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("Không thể nạp số tiền âm");
    }
    assets += amount;
  }

  public void Withdraw(double amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("Số tiền rút không thể âm");
    }
    if (assets >= amount) {
      assets -= amount;
    } else {
      System.out.println("Không đủ tiền để rút");
      throw new IllegalArgumentException("Không đủ tiền để rút");
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    User user = (User) o;

    return Objects.equals(account, user.account);
  }

  @Override
  public int hashCode() {
    return Objects.hash(account);
  }
}
