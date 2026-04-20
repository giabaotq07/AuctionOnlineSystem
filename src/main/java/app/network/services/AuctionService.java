package app.network.services;

import java.util.concurrent.locks.ReentrantLock;

public class AuctionService {
    private static AuctionService instance;
    private final ReentrantLock lock = new ReentrantLock();

    public static synchronized AuctionService getInstance() {
        if (instance == null) instance = new AuctionService();
        return instance;
    }

    public boolean placeBid(int auctionId, String username, double amount) {
        lock.lock();
        try {
            // Giả lập check DB: SELECT current_price FROM items WHERE id = auctionId
            double currentPrice = 100.0;
            if (amount > currentPrice) {
                // UPDATE items SET current_price = amount ...
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
}