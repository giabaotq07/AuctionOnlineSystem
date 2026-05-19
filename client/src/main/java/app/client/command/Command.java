package app.client.command;

import app.client.manager.ClientNotificationCenter;
import app.common.models.PacketRes;

/** Command. */
public abstract class Command {
  /** execute. */
  public abstract void execute(PacketRes packet);

  public void notify(String msg) {
    ClientNotificationCenter.getInstance().notify(msg == null ? "" : msg);
  }
}
