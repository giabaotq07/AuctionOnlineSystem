package app.client.command;

import app.common.models.PacketRes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Command. */
public abstract class Command {
  protected List<Consumer<String>> listeners = new ArrayList<>();

  /** execute. */
  public abstract void execute(PacketRes packet);

  public void notify(String message) {
    for (Consumer<String> listener : listeners) {
      listener.accept(message);
    }
  }

  public void addListener(Consumer<String> listener) {
    listeners.add(listener);
  }

  public void removeListener(Consumer<String> listener) {
    listeners.remove(listener);
  }
}
