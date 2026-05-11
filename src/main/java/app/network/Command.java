package app.network;

import app.models.PacketReq;

public interface Command {
  void execute(ClientHandler clientHandler, PacketReq packet);
}
