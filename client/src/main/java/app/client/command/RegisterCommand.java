package app.client.command;

import app.common.dto.RegisterResponse;
import app.common.models.PacketRes;

/** RegisterCommand. */
public class RegisterCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet.isSuccess()) {
      packet.getData(RegisterResponse.class);
    }
    notify(packet == null ? "" : packet.getMessage());
  }
}
