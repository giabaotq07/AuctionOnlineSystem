package Common;

public class MainForTesting {
    public static void main(String[] args) {
        Auction auctionManager = Auction.getInstance();
        Bid bidEngine = Bid.getInstance();
        auctionManager.xulydulieu();
        auctionManager.showStatus();
    }
}