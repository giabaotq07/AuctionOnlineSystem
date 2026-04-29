package app.models;

import java.util.ArrayList;
import java.util.List;

public class AuctionStateManager implements java.io.Serializable {
  private static final AuctionStateManager instance = new AuctionStateManager();

  private final List<AuctionSession> activeAuctions = new ArrayList<>();
  private final List<AuctionSession> completedAuctions = new ArrayList<>();

  private AuctionStateManager() {}

  public static AuctionStateManager getInstance() {
    return instance;
  }

  public void addSession(AuctionSession session) {
    if (session.getStatus() == AuctionStatus.ACTIVE) {
      activeAuctions.add(session);
    } else {
      completedAuctions.add(session);
    }
  }

  public void updateSessionStatus(AuctionSession session) {
    if (session.getStatus() != AuctionStatus.ACTIVE) {
      activeAuctions.remove(session);
      if (!completedAuctions.contains(session)) {
        completedAuctions.add(session);
      }
    }
  }

  public List<AuctionSession> getActiveAuctions() {
    return new ArrayList<>(activeAuctions);
  }

  public List<AuctionSession> getCompletedAuctions() {
    return new ArrayList<>(completedAuctions);
  }

  public void registerObserverToActive(AuctionObserver observer) {
    for (AuctionSession session : activeAuctions) {
      session.registerObserver(observer);
    }
  }
}
