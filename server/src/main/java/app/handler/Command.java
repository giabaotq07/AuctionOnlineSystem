package app.handler;

import app.models.PacketReq;

/** Command. */
public interface Command {
  /** execute. */
  void execute(ClientHandler clientHandler, PacketReq packet);
}
