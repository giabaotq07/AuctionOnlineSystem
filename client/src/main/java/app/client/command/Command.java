package app.client.command;

import app.common.models.PacketRes;

/** Command. */
public interface Command {
  /** execute. */
  void execute(PacketRes packet);
}
