package app.models;

public class User {
  private String id;
  private String name;
  private String account;
  private double assets;
  private String password;

  public User(String id, String name, String account, String password) {
    this.account = account;
    this.password = password;
    this.assets = 0;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
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

  /*
   public void setAccount(String name) {
       this.account = name;
   }
  */

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setAccount(String account) {}

  public void Deposit(double amount) {
    assets += amount;
  }

  public void Withdraw(double amount) {
    if (assets >= amount) {
      assets -= amount;
    } else System.out.println("Không đủ tiền để rút");
  }
}
