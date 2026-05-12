package app.models;

import app.data.AuctionSummary;
import app.data.AuctionsRequest;
import app.data.AuctionsResponse;
import app.enums.PacketType;
import app.exception.ConnectException;
import app.network.Client;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataStore {
  static Logger logger = LoggerFactory.getLogger(DataStore.class);
  public List<AuctionSummary> sessions;
  public User currentUser; // Track the logged-in user
  public Auction currentAuction;
  private static DataStore instance;

  private DataStore() {
    sessions = new ArrayList<>();
    logger.info("DataStore instance created");
  }

  public static DataStore getInstance() {
    if (instance == null) {
      synchronized (DataStore.class) {
        if (instance == null) {
          instance = new DataStore();
          instance.loadSessions();
          try {
          Client.getInstance()
              .sendRequest(PacketReq.of(PacketType.FETCH_AUCTIONS, new AuctionsRequest()));
          } catch (IOException e) {
            throw new ConnectException(e.getMessage());
          }
          return instance;
        }
      }
    }
    return instance;
  }

  void loadSessions() {
    Client.getInstance()
        .subscribe(
            PacketType.FETCH_AUCTIONS,
            (AuctionsResponse response) ->
                Platform.runLater(
                    () -> {
                      if (response.success() && response.auctions() != null) {
                        sessions = response.auctions();
                      }
                    }));
  }
}
