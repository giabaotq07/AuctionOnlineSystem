package app;

import app.dto.AuctionSummary;
import app.dto.UserData;
import app.dto.WalletUpdateResponse;
import app.enums.PacketType;
import app.mapper.DtoMapper;
import app.models.Auction;
import app.models.User;
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
  private static volatile DataStore instance;

  private DataStore() {
    auctions = new ArrayList<>();
    logger.info("DataStore instance created");
    loadWalletUpdates();
  }

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

  /** updateCurrentUser. */
  public void updateCurrentUser(UserData data) {
    if (data == null) {
      return;
    }
    User user = DtoMapper.toUser(data);
    currentUser = user;
    Client.getInstance().setCurrentUser(user);
  }

  void loadWalletUpdates() {
    Client.getInstance()
        .subscribe(
            PacketType.WALLET_UPDATE,
            WalletUpdateResponse.class,
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
