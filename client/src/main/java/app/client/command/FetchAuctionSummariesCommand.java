package app.client.command;

import app.client.store.AuctionStore;
import app.common.dto.AuctionPreview;
import app.common.dto.AuctionSummariesResponse;
import app.common.protocol.PacketRes;

/** FetchAuctionSummariesCommand. */
public class FetchAuctionSummariesCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet.isSuccess()) {
      AuctionSummariesResponse response = packet.getData(AuctionSummariesResponse.class);
      if (response != null && response.auctions() != null) {
        for (AuctionPreview auction : response.auctions()) {
          AuctionStore.getInstance().addPreview(auction);
        }
      }
      notifyUpdate();
    }
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
