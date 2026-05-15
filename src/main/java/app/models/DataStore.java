package app.models;

import app.data.AuctionSummary;
import app.data.AuctionsRequest;
import app.data.AuctionsResponse;
import app.data.UserData;
import app.data.WalletUpdateResponse;
import app.enums.OperationStatus;
import app.enums.PacketType;
import app.exception.ConnectException;
import app.network.Client;
import app.utils.AlertUtils;
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

  /** updateCurrentUser. */
  public void updateCurrentUser(UserData data) {
    if (data == null) {
      return;
    }
    User user = UserFactory.createUser(data);
    currentUser = user;
    Client.getInstance().setCurrentUser(user);
  }

  void loadAuctions() {
    Client.getInstance()
        .subscribe(
            PacketType.FETCH_AUCTIONS,
            (AuctionsResponse response) ->
                Platform.runLater(
                    () -> {
                      if (response.success() && response.auctions() != null) {
                        auctions = response.auctions();
                      }
                    }));
  }

  void loadWalletUpdates() {
    Client.getInstance()
        .subscribe(
            PacketType.WALLET_UPDATE,
            (WalletUpdateResponse response) ->
                Platform.runLater(
                    () -> {
                      if (response == null) {
                        return;
                      }
                      if (response.status() != OperationStatus.SUCCESS) {
                        AlertUtils.showError("Ví", response.message());
                        return;
                      }
                      if (response.user() != null) {
                        updateCurrentUser(response.user());
                      }
                    }));
  }
}
