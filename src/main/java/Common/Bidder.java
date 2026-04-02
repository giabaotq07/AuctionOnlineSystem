package Common;

public class Bidder extends User {

  private double balance;

  public Bidder(String username, String password, String fullName, double balance) {
    super(username, password, fullName, "BIDDER"); // set role cứng
    this.balance = balance;
  }

  public boolean canAfford(double amount) {
    if (amount > balance) return false;
    else return true;
  }

  public double getBalance() {
    return balance;
  }

  public void deposit(double amount) {
    if (amount > 0) {
      balance += amount;
    }
  }

  public boolean placeBid(double amount) {
    if (amount <= balance) {
      balance -= amount;
      return true;
    }
    return false;
  }

  @Override
  public void printInfo() {
    super.printInfo();
    System.out.println("Balance: " + balance);
  }
}
