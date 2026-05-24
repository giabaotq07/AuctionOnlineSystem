package app.client.command;

import app.common.dto.RegisterResponse;
import app.common.protocol.PacketRes;

/** RegisterCommand. */
public class RegisterCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet.isSuccess()) {
      RegisterResponse response = packet.getData(RegisterResponse.class);
      if (response != null && response.user() != null) {
        // user registered
      }
    }
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
