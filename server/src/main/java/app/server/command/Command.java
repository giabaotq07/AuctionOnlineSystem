package app.server.command;

import app.common.models.PacketReq;
import app.server.network.ClientHandler;

/** Command. */
public interface Command {
  /** execute. */
  void execute(ClientHandler clientHandler, PacketReq packet);
}
