package app.client.command;

import app.common.dto.AuctionSummariesResponse;
import app.common.models.PacketRes;

/** FetchAuctionSummariesCommand. */
public class FetchAuctionSummariesCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet.isSuccess()) {
      packet.getData(AuctionSummariesResponse.class);
    }
    notify(packet.getMessage());
  }
}
