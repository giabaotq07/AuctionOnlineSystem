package app.network;

import app.enums.Result;
import app.models.ResponsePacket;

public class PlaceBidCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, ResponsePacket<?> packet) {
    // Implement place bid logic based on packet payload
    System.out.println(
        "[SERVER] Handling PLACE_BID command from user: " + clientHandler.getUsername());
    // For now, simulating success
    ResponsePacket<String> responsePacket =
        new ResponsePacket<>(Result.SUCCESS, "BidTransaction placed successfully.");
    responsePacket.setMessage("System");
    clientHandler.sendMessage(responsePacket);
  }
}
