package app.client.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Single notification hub for completed client commands. */
public final class ClientNotificationCenter {
  private static volatile ClientNotificationCenter instance;

  private final List<Consumer<String>> listeners = new ArrayList<>();

  private ClientNotificationCenter() {}

  /** getInstance. */
  public static ClientNotificationCenter getInstance() {
    if (instance == null) {
      synchronized (ClientNotificationCenter.class) {
        if (instance == null) {
          instance = new ClientNotificationCenter();
        }
      }
    }
    return instance;
  }

  /** addListener. */
  public synchronized void addListener(Consumer<String> listener) {
    listeners.add(listener);
  }

  /** removeListener. */
  public synchronized void removeListener(Consumer<String> listener) {
    listeners.remove(listener);
  }

  /** notify. */
  public void notify(String message) {
    List<Consumer<String>> snapshot;
    synchronized (this) {
      snapshot = new ArrayList<>(listeners);
    }
    for (Consumer<String> listener : snapshot) {
      listener.accept(message);
    }
  }
}
