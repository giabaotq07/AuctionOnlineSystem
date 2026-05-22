package app.client.command;

import app.client.store.AuctionStore;
import app.common.dto.CreateAuctionResponse;
import app.common.protocol.PacketRes;

/** CreateAuctionCommand. */
public class CreateAuctionCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet.isSuccess()) {
      CreateAuctionResponse response = packet.getData(CreateAuctionResponse.class);
      if (response != null && response.auction() != null) {
        AuctionStore.getInstance()
            .addDetail(app.common.mapper.ModelMapper.toAuctionModel(response.auction()));
      }
      notifyUpdate();
    }
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
