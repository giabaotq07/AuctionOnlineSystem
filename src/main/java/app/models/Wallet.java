package app.models;

public class Wallet {
    private double assets;
    public Wallet() {
        this.assets = 0;
    }
    public Wallet(double assets) {
        this.assets = assets;
    }

    // Dùng synchronized để tại một thời điểm chỉ một luồng được phép sửa tiền
    public synchronized boolean withdraw(double amount) {
        if (amount > 0 && assets >= amount) {
            this.assets -= amount;
            return true; // Rút tiền/Đặt cọc thành công
        }
        return false; // Không đủ tiền
    }

    public synchronized void deposit(double amount) {
        if (amount > 0) {
            this.assets += amount;
        }
    }

    public double getAssets() {
        return assets;
    }
}
