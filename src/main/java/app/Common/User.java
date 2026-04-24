package app.Common;

import javax.naming.InsufficientResourcesException;

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
        if (amount < 0) {
            throw new IllegalArgumentException("Không thể nạp số tiền âm");
        }

        assets += amount;
    }
    public void Withdraw(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Số tiền rút không thể âm");
        }

        if (amount > assets) {
            throw new IllegalArgumentException("Không đủ tiền để rút");
        }

        assets -= amount;
    }
}
