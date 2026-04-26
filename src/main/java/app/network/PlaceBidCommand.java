package app.network;

import app.models.CommandType;
import app.models.MessagePacket;

public class PlaceBidCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, MessagePacket<?> packet) {
    // Implement place bid logic based on packet payload
    System.out.println(
        "[SERVER] Handling PLACE_BID command from user: " + clientHandler.getUsername());
    // For now, simulating success
    MessagePacket<String> response =
        new MessagePacket<>(CommandType.SUCCESS, "Bid placed successfully.");
    response.setMessage("System");
    clientHandler.sendMessage(response);
  }
}
