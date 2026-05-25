package app.client.manager;

import static org.junit.jupiter.api.Assertions.*;

import app.common.dto.ChatResponse;
import app.common.dto.UserDto;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

public class ClientNotificationCenterTest {

  @Test
  public void testSingleton() {
    ClientNotificationCenter hub1 = ClientNotificationCenter.getInstance();
    ClientNotificationCenter hub2 = ClientNotificationCenter.getInstance();
    assertNotNull(hub1);
    assertSame(hub1, hub2);
  }

  @Test
  public void testMessageListeners() {
    ClientNotificationCenter center = ClientNotificationCenter.getInstance();
    AtomicReference<String> msgRef = new AtomicReference<>();
    Consumer<String> listener = msgRef::set;

    center.addMessageListener(listener);
    center.notifyMessage("Hello World");
    assertEquals("Hello World", msgRef.get());

    center.removeMessageListener(listener);
    center.notifyMessage("Silent Message");
    assertEquals("Hello World", msgRef.get()); // Unchanged
  }

  @Test
  public void testLegacyMessageListeners() {
    ClientNotificationCenter center = ClientNotificationCenter.getInstance();
    AtomicReference<String> msgRef = new AtomicReference<>();
    Consumer<String> listener = msgRef::set;

    center.addListener(listener);
    center.notify("Legacy Hello");
    assertEquals("Legacy Hello", msgRef.get());

    center.removeListener(listener);
    center.notify("Silent Legacy");
    assertEquals("Legacy Hello", msgRef.get());
  }

  @Test
  public void testChatListeners() {
    ClientNotificationCenter center = ClientNotificationCenter.getInstance();
    AtomicReference<ChatResponse> chatRef = new AtomicReference<>();
    Consumer<ChatResponse> listener = chatRef::set;

    center.addChatListener(listener);
    ChatResponse response = new ChatResponse(1, "john", "hello", LocalDateTime.now());
    center.notifyChat(response);
    assertEquals(response, chatRef.get());

    // Null case check
    center.notifyChat(null);
    assertEquals(response, chatRef.get()); // Keeps old value

    center.removeChatListener(listener);
    ChatResponse newResponse = new ChatResponse(2, "alice", "hi", LocalDateTime.now());
    center.notifyChat(newResponse);
    assertEquals(response, chatRef.get());
  }

  @Test
  public void testUpdateListeners() {
    ClientNotificationCenter center = ClientNotificationCenter.getInstance();
    AtomicBoolean updated = new AtomicBoolean(false);
    Runnable listener = () -> updated.set(true);

    center.addUpdateListener(listener);
    center.notifyUpdate();
    assertTrue(updated.get());

    center.removeUpdateListener(listener);
    updated.set(false);
    center.notifyUpdate();
    assertFalse(updated.get());
  }

  @Test
  public void testUserListListeners() {
    ClientNotificationCenter center = ClientNotificationCenter.getInstance();
    AtomicReference<List<UserDto>> usersRef = new AtomicReference<>();
    Consumer<List<UserDto>> listener = usersRef::set;

    center.addUserListListener(listener);
    List<UserDto> users = new ArrayList<>();
    center.notifyUserList(users);
    assertEquals(users, usersRef.get());

    // Null case check
    center.notifyUserList(null);
    assertEquals(users, usersRef.get());

    center.removeUserListListener(listener);
    List<UserDto> newUsers = new ArrayList<>();
    center.notifyUserList(newUsers);
    assertEquals(users, usersRef.get());
  }
}
