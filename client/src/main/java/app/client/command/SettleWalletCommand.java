package app.client.command;

import app.common.models.PacketRes;

/** SettleWalletCommand. */
public class SettleWalletCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
