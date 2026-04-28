package app.network;

import app.models.MessagePacket;

public interface Command {
  void execute(ClientHandler clientHandler, MessagePacket<?> packet);
}
