package app.models;

import app.dto.AuctionSummariesResponse;
import app.dto.AuctionSummary;
import app.dto.UserData;
import app.dto.WalletUpdateResponse;
import app.enums.PacketType;
import app.exception.ConnectException;
import app.mapper.DtoMapper;
import app.network.Client;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** DataStore. */
public class DataStore {
  static Logger logger = LoggerFactory.getLogger(DataStore.class);
  public List<AuctionSummary> auctions;
  public User currentUser;
  public Auction currentAuction;
  private static DataStore instance;

  private DataStore() {
    auctions = new ArrayList<>();
    logger.info("DataStore instance created");
    loadAuctions();
    loadWalletUpdates();
  }

  /** getInstance. */
  public static DataStore getInstance() {
    if (instance == null) {
      synchronized (DataStore.class) {
        if (instance == null) {
          instance = new DataStore();
          try {
            Client.getInstance().sendRequest(PacketReq.of(PacketType.FETCH_AUCTION_SUMMARIES));
          } catch (IOException e) {
            throw new ConnectException(e.getMessage());
          }
          return instance;
        }
      }
    }
    return instance;
  }

  /** updateCurrentUser. */
  public void updateCurrentUser(UserData data) {
    if (data == null) {
      return;
    }
    User user = DtoMapper.toUser(data);
    currentUser = user;
    Client.getInstance().setCurrentUser(user);
  }

  void loadAuctions() {
    Client.getInstance()
        .subscribe(
            PacketType.FETCH_AUCTION_SUMMARIES,
            (AuctionSummariesResponse response, boolean success, String message) ->
                Platform.runLater(
                    () -> {
                      if (success && response != null && response.auctions() != null) {
                        auctions = response.auctions();
                      }
                    }));
  }

  void loadWalletUpdates() {
    Client.getInstance()
        .subscribe(
            PacketType.WALLET_UPDATE,
            (WalletUpdateResponse response, boolean success, String message) ->
                Platform.runLater(
                    () -> {
                      if (!success || response == null) {
                        return;
                      }
                      if (response.user() != null) {
                        updateCurrentUser(response.user());
                      }
                    }));
  }
}
