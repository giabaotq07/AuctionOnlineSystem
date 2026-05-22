package app.client.command;

import app.common.protocol.PacketRes;

/** ErrorCommand. */
public class ErrorCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
