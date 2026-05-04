package app.network;

import app.models.ResponsePacket;

public class PlaceBidCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, ResponsePacket<?> packet) {
    // Implement place bid logic based on packet payload
  }
}
