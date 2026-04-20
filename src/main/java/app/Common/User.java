package app.Common;

public class User {
    private String id;
    private String name;
    private double assets;
    public User(String id , String name) {
        this.id = id;
        this.name = name;
        this.assets = 0;
    }
    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public double getAssets() {
        return assets;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void Deposit(double amount) {
        assets += amount;
    }
    public void Withdraw(double amount) {
        if (assets >= amount) {
            assets -= amount;
        }
         else System.out.println("Không đủ tiền để rút");
    }
}
