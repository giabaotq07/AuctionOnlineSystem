package app.client.command;

import app.common.models.PacketRes;

/** SettleWalletCommand. */
public class SettleWalletCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    notify(packet == null ? "" : packet.getMessage());
  }
}
