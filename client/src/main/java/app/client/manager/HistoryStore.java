package app.client.manager;

import app.common.dto.AuctionHistoryResponse;
import app.common.dto.AuctionSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HistoryStore {
  private static volatile HistoryStore instance;
  private final Map<Integer, AuctionSummary> historyCache = new ConcurrentHashMap<>();

  private HistoryStore() {}

  /** getInstance. */
  public static HistoryStore getInstance() {
    if (instance == null) {
      synchronized (HistoryStore.class) {
        if (instance == null) {
          instance = new HistoryStore();
        }
      }
    }
    return instance;
  }

  public void handleHistoryResponse(
      AuctionHistoryResponse response, boolean success, String message) {
    if (success) {
      resolveHistory(response);
    }
  }

  private void resolveHistory(AuctionHistoryResponse response) {
    if (response != null) {
      if (response.auctions() != null) {
        historyCache.clear();
        for (AuctionSummary summary : response.auctions()) {
          historyCache.put(summary.auctionId(), summary);
        }
      }
    }
  }

  public List<AuctionSummary> getAuctionHistory() {
    List<AuctionSummary> auctionSummaries = new ArrayList<>();
    for (Map.Entry<Integer, AuctionSummary> entry : historyCache.entrySet()) {
      auctionSummaries.add(entry.getValue());
    }
    return auctionSummaries;
  }
}
