package app.client.command;

import app.client.store.AuctionStore;
import app.client.store.LiveAuctionSessionStore;
import app.common.dto.SetAutoBidResponse;
import app.common.protocol.PacketRes;

/** SetAutoBidCommand. */
public class SetAutoBidCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet != null && packet.isSuccess()) {
      SetAutoBidResponse response = packet.getData(SetAutoBidResponse.class);
      if (response != null) {
        AuctionStore.getInstance()
            .updateBid(response.auctionId(), response.highestBid(), response.leadingBidderId());
        LiveAuctionSessionStore.getInstance()
            .setActiveAutoBid(response.maxAmount(), response.incrementAmount(), response.enabled());
      }
      notifyUpdate();
    }
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
