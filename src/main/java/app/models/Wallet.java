package app.models;

public class Wallet implements java.io.Serializable {
  private long assets;

  public Wallet() {
    this.assets = 0;
  }

  public Wallet(long assets) {
    this.assets = assets;
  }

  public synchronized void withdraw(long amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("So tien rut phai la so duong.");
    }
    if (assets < amount) {
      throw new IllegalArgumentException("So tien rut vuot qua so du.");
    }
    this.assets -= amount;
  }

  public synchronized void deposit(long amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("So tien gui phai la so duong.");
    }
    this.assets += amount;
  }

  public long getAssets() {
    return assets;
  }
}
