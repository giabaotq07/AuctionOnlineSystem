package app.client.manager;

import app.common.dto.ChatResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Single notification hub for completed client commands. */
public final class ClientNotificationCenter {
  private static volatile ClientNotificationCenter instance;

  private final List<Consumer<String>> messageListeners = new ArrayList<>();
  private final List<Consumer<ChatResponse>> chatListeners = new ArrayList<>();
  private final List<Runnable> updateListeners = new ArrayList<>();
  private final List<Consumer<List<app.common.dto.UserDto>>> userListListeners = new ArrayList<>();

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

  /** addMessageListener. */
  public synchronized void addMessageListener(Consumer<String> listener) {
    messageListeners.add(listener);
  }

  /** removeMessageListener. */
  public synchronized void removeMessageListener(Consumer<String> listener) {
    messageListeners.remove(listener);
  }

  /** addChatListener. */
  public synchronized void addChatListener(Consumer<ChatResponse> listener) {
    chatListeners.add(listener);
  }

  /** removeChatListener. */
  public synchronized void removeChatListener(Consumer<ChatResponse> listener) {
    chatListeners.remove(listener);
  }

  /** addUpdateListener. */
  public synchronized void addUpdateListener(Runnable listener) {
    updateListeners.add(listener);
  }

  /** removeUpdateListener. */
  public synchronized void removeUpdateListener(Runnable listener) {
    updateListeners.remove(listener);
  }

  /** notifyMessage. */
  public void notifyMessage(String message) {
    List<Consumer<String>> snapshot;
    synchronized (this) {
      snapshot = new ArrayList<>(messageListeners);
    }
    for (Consumer<String> listener : snapshot) {
      listener.accept(message);
    }
  }

  /** notifyChat. */
  public void notifyChat(ChatResponse response) {
    if (response == null) {
      return;
    }
    List<Consumer<ChatResponse>> snapshot;
    synchronized (this) {
      snapshot = new ArrayList<>(chatListeners);
    }
    for (Consumer<ChatResponse> listener : snapshot) {
      listener.accept(response);
    }
  }

  /** notifyUpdate. */
  public void notifyUpdate() {
    List<Runnable> snapshot;
    synchronized (this) {
      snapshot = new ArrayList<>(updateListeners);
    }
    for (Runnable listener : snapshot) {
      listener.run();
    }
  }

  /** addListener. */
  public synchronized void addListener(Consumer<String> listener) {
    addMessageListener(listener);
  }

  /** removeListener. */
  public synchronized void removeListener(Consumer<String> listener) {
    removeMessageListener(listener);
  }

  /** notify. */
  public void notify(String message) {
    notifyMessage(message);
  }

  public synchronized void addUserListListener(Consumer<List<app.common.dto.UserDto>> listener) {
    userListListeners.add(listener);
  }

  public synchronized void removeUserListListener(Consumer<List<app.common.dto.UserDto>> listener) {
    userListListeners.remove(listener);
  }

  public void notifyUserList(List<app.common.dto.UserDto> users) {
    if (users == null) {
      return;
    }
    List<Consumer<List<app.common.dto.UserDto>>> snapshot;
    synchronized (this) {
      snapshot = new ArrayList<>(userListListeners);
    }
    for (Consumer<List<app.common.dto.UserDto>> listener : snapshot) {
      listener.accept(users);
    }
  }
}
