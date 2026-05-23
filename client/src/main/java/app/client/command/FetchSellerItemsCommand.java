package app.client.command;

import app.common.protocol.PacketRes;

/** FetchSellerItemsCommand. */
public class FetchSellerItemsCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet != null && packet.isSuccess()) {
      notifyUpdate();
      return;
    }
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
