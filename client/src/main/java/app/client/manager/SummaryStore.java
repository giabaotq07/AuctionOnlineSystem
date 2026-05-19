package app.client.manager;

import app.common.dto.AuctionSummariesResponse;
import app.common.dto.AuctionSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SummaryStore extends DataStore {
  private final Map<Integer, AuctionSummary> summaryCache = new ConcurrentHashMap<>();

  private static volatile SummaryStore instance;

  private SummaryStore() {}

  /** getInstance. */
  public static SummaryStore getInstance() {
    if (instance == null) {
      synchronized (SummaryStore.class) {
        if (instance == null) {
          instance = new SummaryStore();
        }
      }
    }
    return instance;
  }

  public void handleSummaryResponse(
      AuctionSummariesResponse response, boolean success, String message) {
    if (success) {
      resolveSummaries(response);
    }
  }

  private void resolveSummaries(AuctionSummariesResponse response) {
    if (response != null) {
      if (response.auctions() != null) {
        summaryCache.clear();
        for (AuctionSummary summary : response.auctions()) {
          summaryCache.put(summary.auctionId(), summary);
        }
      }
    }
  }

  public List<AuctionSummary> getAuctionSummaries() {
    List<AuctionSummary> auctionSummaries = new ArrayList<>();
    for (Map.Entry<Integer, AuctionSummary> entry : summaryCache.entrySet()) {
      auctionSummaries.add(entry.getValue());
    }
    return auctionSummaries;
  }
}
