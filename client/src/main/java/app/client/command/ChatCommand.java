package app.client.command;

import app.common.models.PacketRes;

/** ChatCommand. */
public class ChatCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
