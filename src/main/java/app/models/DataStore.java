package app.models;

import app.data.AuctionSummary;
import app.data.AuctionsRequest;
import app.data.AuctionsResponse;
import app.enums.PacketType;
import app.network.Client;
import app.utils.JsonUtil;
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

  public static DataStore getInstance() throws IOException {
    if (instance == null) {
      synchronized (DataStore.class) {
        if (instance == null) {
          instance = new DataStore();
          instance.loadSessions();
          logger.info("DataStore send");
          Client.getInstance()
              .sendRequest(
                  new Packet(PacketType.FETCH_AUCTIONS, JsonUtil.toJson(new AuctionsRequest())));
          logger.info("DataStore sended");
          return instance;
        }
      }
    }
    return instance;
  }

  void loadSessions() {
    Client.getInstance()
        .setOnMessageReceived(
            packet ->
                Platform.runLater(
                    () -> {
                      if (packet.getType() == PacketType.FETCH_AUCTIONS) {
                        AuctionsResponse response =
                            JsonUtil.fromJson(packet.getData(), AuctionsResponse.class);
                        logger.info("AuctionsResponse received11111111");
                        if (response.success() && response.auctions() != null) {
                          sessions = response.auctions();
                        }
                      }
                    }));
  }
}
