package app.client.command;

import app.client.store.LiveAuctionSessionStore;
import app.client.store.AuctionStore;
import app.common.dto.AuctionDetailResponse;
import app.common.protocol.PacketRes;

/** FetchAuctionDetailCommand. */
public class FetchAuctionDetailCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet.isSuccess()) {
      AuctionDetailResponse response = packet.getData(AuctionDetailResponse.class);
      if (response != null && response.auction() != null) {
        AuctionStore.getInstance()
            .addDetail(app.common.mapper.ModelMapper.toAuctionModel(response.auction()));
      }
      if (response != null) {
        LiveAuctionSessionStore.getInstance().finishDetailRequest(response.auctionId());
      }
      notifyUpdate();
    }
    //    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
