package app.client.command;

import app.client.store.LiveAuctionSessionStore;
import app.common.dto.DisableAutoBidResponse;
import app.common.protocol.PacketRes;

/** DisableAutoBidCommand. */
public class DisableAutoBidCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet != null && packet.isSuccess()) {
      DisableAutoBidResponse response = packet.getData(DisableAutoBidResponse.class);
      if (response != null) {
        LiveAuctionSessionStore.getInstance().setActiveAutoBid(0L, 0L, response.enabled());
      }
      notifyUpdate();
    }
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
