package app.client.command;

import app.client.manager.LiveAuctionSessionStore;
import app.client.store.AuctionStore;
import app.client.store.ItemStore;
import app.common.dto.AuctionDetail;
import app.common.dto.AuctionDetailResponse;
import app.common.mapper.DtoMapper;
import app.common.protocol.PacketRes;

/** FetchAuctionDetailCommand. */
public class FetchAuctionDetailCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet.isSuccess()) {
      AuctionDetailResponse response = packet.getData(AuctionDetailResponse.class);
      if (response != null && response.detail() != null) {
        AuctionDetail detail = response.detail();
        AuctionStore.getInstance().addAuction(DtoMapper.toAuction(detail.auction()));
        ItemStore.getInstance().addItem(DtoMapper.toItem(detail.item()));
        LiveAuctionSessionStore.getInstance().setSelectedDetail(detail);
      }
      notifyUpdate();
    }
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
