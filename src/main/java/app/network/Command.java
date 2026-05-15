package app.network;

import app.models.PacketReq;

/** Command. */
public interface Command {
  /** execute. */
  void execute(ClientHandler clientHandler, PacketReq packet);
}
