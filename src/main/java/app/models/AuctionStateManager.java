package app.models;

import app.enums.AuctionStatus;
import java.util.ArrayList;
import java.util.List;

public class AuctionStateManager implements java.io.Serializable {
  private static final AuctionStateManager instance = new AuctionStateManager();

  private final List<Auction> activeAuctions = new ArrayList<>();
  private final List<Auction> completedAuctions = new ArrayList<>();

  private AuctionStateManager() {}

  public static AuctionStateManager getInstance() {
    return instance;
  }

  public void addSession(Auction session) {
    if (session.getStatus() == AuctionStatus.OPEN) {
      activeAuctions.add(session);
    } else {
      completedAuctions.add(session);
    }
  }

  public void updateSessionStatus(Auction session) {
    if (session.getStatus() != AuctionStatus.OPEN) {
      activeAuctions.remove(session);
      if (!completedAuctions.contains(session)) {
        completedAuctions.add(session);
      }
    }
  }

  public List<Auction> getActiveAuctions() {
    return new ArrayList<>(activeAuctions);
  }

  public List<Auction> getCompletedAuctions() {
    return new ArrayList<>(completedAuctions);
  }

  public void registerObserverToActive(AuctionObserver observer) {
    for (Auction session : activeAuctions) {
      session.registerObserver(observer);
    }
  }
}
