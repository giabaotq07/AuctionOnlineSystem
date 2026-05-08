package app.network;

import app.models.Packet;

public interface Command {
  void execute(ClientHandler clientHandler, Packet packet);
}
