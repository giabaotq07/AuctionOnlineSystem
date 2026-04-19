package app.models;

import javafx.stage.Stage;

public class User {
    private String id;
    private String username;
    private double assets;
    private String password;
    public User(String id , String username , String password) {
        this.username = username;
        this.password = password;
        this.assets = 0;
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
    public String getName() {
        return username;
    }
    public double getAssets() {
        return assets;
    }
    public void setName(String name) {
        this.username = name;
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
