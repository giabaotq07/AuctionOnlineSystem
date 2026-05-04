package app.network;

import app.models.Packet;

public class PlaceBidCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, Packet packet) {
    // Implement place bid logic based on packet payload
  }
}
