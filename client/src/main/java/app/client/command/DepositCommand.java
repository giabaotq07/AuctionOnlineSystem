package app.client.command;

import app.common.models.PacketRes;

/** DepositCommand. */
public class DepositCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
