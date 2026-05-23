package app.client.command;

import app.common.protocol.PacketRes;

/** DisableAutoBidCommand. */
public class DisableAutoBidCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet != null && packet.isSuccess()) {
      notifyUpdate();
    }
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
