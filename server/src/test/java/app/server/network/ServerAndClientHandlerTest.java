package app.server.network;

import static org.junit.jupiter.api.Assertions.*;

import app.common.enums.RequestType;
import app.common.enums.ResponseType;
import app.common.models.User;
import app.common.protocol.PacketRes;
import app.server.ServerApp;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * ServerAndClientHandlerTest. Kiem thu mang Socket Server va ClientHandler su dung subclass Socket
 * va truyen tham so null cho cac service nham tranh loi mock class Mockito tren Java 25.
 */
public class ServerAndClientHandlerTest {

  private Socket customSocket;
  private ClientHandler clientHandler;
  private ByteArrayOutputStream bos;

  @BeforeEach
  void setUp() throws Exception {
    bos = new ByteArrayOutputStream();
    // Su dung subclass thuc te cua Socket thay vi mock de tuong thich 100% voi JDK 25
    customSocket =
        new Socket() {
          @Override
          public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(new byte[0]);
          }

          @Override
          public OutputStream getOutputStream() throws IOException {
            return bos;
          }
        };

    // Truyen null cho tat ca cac tham so Service de tranh Mockito mock tren Java 25
    clientHandler = new ClientHandler(customSocket, null, null, null, null, null, null);
  }

  @Test
  public void testClientHandlerBasics() {
    assertNotNull(clientHandler.getSession());
    assertEquals(customSocket, clientHandler.getSocket());
    assertFalse(clientHandler.isAuthenticated());
    assertNull(clientHandler.getUser());

    // Test close
    assertDoesNotThrow(() -> clientHandler.close());
  }

  @Test
  public void testAuthorizeRejectsBannedAuthenticatedUser() throws Exception {
    User user = app.TestFixtures.user("banned_user", app.common.enums.UserRole.BIDDER);
    user.setId(24);
    user.ban();
    clientHandler.getSession().authenticate(user);

    Method authorize = ClientHandler.class.getDeclaredMethod("authorize", RequestType.class);
    authorize.setAccessible(true);

    assertFalse((boolean) authorize.invoke(clientHandler, RequestType.FETCH_AUCTION_SUMMARIES));
    assertFalse((boolean) authorize.invoke(clientHandler, RequestType.DEPOSIT));
  }

  @Test
  public void testServerClientRegistrationAndBroadcast() throws Exception {
    // Lay private map 'authenticatedClients' cua Server bang Reflection de test tinh nang dang ky
    // va broadcast
    Field field = Server.class.getDeclaredField("authenticatedClients");
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<Integer, ClientHandler> authenticatedClients =
        (Map<Integer, ClientHandler>) field.get(null);

    // Xoa sach map truoc khi test
    authenticatedClients.clear();

    User user = app.TestFixtures.user("john_doe", app.common.enums.UserRole.BIDDER);
    user.setId(42);

    // Mo phong authenticated
    clientHandler.getSession().authenticate(user);

    Server.registerClient(42, clientHandler);

    assertTrue(Server.isUserOnline(42));
    assertEquals(1, Server.getOnlineUserCount());

    // Gửi tin nhắn đơn lẻ
    PacketRes packet = PacketRes.of(ResponseType.CHAT_RESULT, "OK", "Hello");
    assertDoesNotThrow(() -> Server.sendPacketToUser(42, packet));
    assertDoesNotThrow(() -> Server.sendToUser(42, packet));

    // Broadcast
    assertDoesNotThrow(() -> Server.broadcast(packet, -1));
    assertDoesNotThrow(() -> Server.broadcast(packet, 42)); // loại trừ 42

    // Broadcast tới người đang xem phiên đấu giá
    clientHandler.getSession().setViewingAuctionId(99);
    assertDoesNotThrow(() -> Server.broadcastToAuctionViewers(99, packet, -1));

    // Xóa client
    Server.removeClient(42, clientHandler);
    assertFalse(Server.isUserOnline(42));
    assertEquals(0, Server.getOnlineUserCount());
  }

  @Test
  public void testServerAppInstantiation() {
    // Kiem tra rang lop khoi chay ServerApp co the duoc tao ma khong co loi
    ServerApp app = new ServerApp();
    assertNotNull(app);
  }
}
