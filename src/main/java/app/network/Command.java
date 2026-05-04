package app.network;

import app.models.ResponsePacket;

public interface Command {
  void execute(ClientHandler clientHandler, ResponsePacket<?> packet);
}
