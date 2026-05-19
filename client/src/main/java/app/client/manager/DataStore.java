package app.client.manager;

import app.common.dto.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Opens auction details and routes to the live auction view. */
public final class DataStore {
  private static final Logger logger = LoggerFactory.getLogger(DataStore.class);
  private static volatile DataStore instance;

  private final Map<Integer, AuctionSummary> summaryCache = new ConcurrentHashMap<>();
  private final Map<Integer, AuctionSummary> historyCache = new ConcurrentHashMap<>();

  private DataStore() {}

  /** getInstance. */
  public static DataStore getInstance() {
    if (instance == null) {
      synchronized (DataStore.class) {
        if (instance == null) {
          instance = new DataStore();
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

  public List<AuctionSummary> getAuctionHistory() {
    List<AuctionSummary> auctionSummaries = new ArrayList<>();
    for (Map.Entry<Integer, AuctionSummary> entry : historyCache.entrySet()) {
      auctionSummaries.add(entry.getValue());
    }
    return auctionSummaries;
  }
}
