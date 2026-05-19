package app.server.handler;

import app.common.models.PacketReq;

/** Command. */
public interface Command {
  /** execute. */
  void execute(ClientHandler clientHandler, PacketReq packet);
}
