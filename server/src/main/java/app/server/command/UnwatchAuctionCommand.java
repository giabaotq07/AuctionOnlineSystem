package app.server.command;

import app.common.enums.ResponseType;
import app.common.protocol.PacketReq;
import app.server.network.ClientHandler;
import app.server.network.Session;

/** Clears current live-auction watcher context for a client session. */
public class UnwatchAuctionCommand extends SafeCommand {
  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) {
    Session session = clientHandler.getSession();
    if (session == null) {
      return;
    }
    session.setViewingAuctionId(null);
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.ERROR;
  }

  @Override
  protected String unexpectedErrorMessage() {
    return "Không thể bỏ theo dõi phiên đấu giá.";
  }
}
