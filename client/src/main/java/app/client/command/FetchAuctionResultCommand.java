package app.client.command;

import app.client.store.AuctionStore;
import app.common.dto.AuctionResultResponse;
import app.common.models.PacketRes;

/** FetchAuctionResultCommand. */
public class FetchAuctionResultCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet.isSuccess()) {
      AuctionResultResponse response = packet.getData(AuctionResultResponse.class);
      if (response != null) {
        AuctionStore.getInstance().markFinished(response.auctionId(), response.finalPrice());
      }
    }
    notify(packet.getMessage());
  }
}
