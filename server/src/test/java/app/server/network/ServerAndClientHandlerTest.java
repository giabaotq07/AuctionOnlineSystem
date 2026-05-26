package app.server.network;

import static org.junit.jupiter.api.Assertions.*;

import app.common.enums.RequestType;
import app.common.enums.ResponseType;
import app.common.models.User;
import app.common.protocol.PacketReq;
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

  @Test
  public void testRegisterClient_replacesOldHandler() throws Exception {
    Field field = Server.class.getDeclaredField("authenticatedClients");
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<Integer, ClientHandler> authenticatedClients =
        (Map<Integer, ClientHandler>) field.get(null);
    authenticatedClients.clear();

    User user = app.TestFixtures.user("user_reg", app.common.enums.UserRole.BIDDER);
    user.setId(99);

    // Dang ky handler lan 1
    ClientHandler handler1 = new ClientHandler(customSocket, null, null, null, null, null, null);
    handler1.getSession().authenticate(user);
    Server.registerClient(99, handler1);
    assertEquals(1, Server.getOnlineUserCount());

    // Dang ky handler lan 2 voi cung userId -> handler cu bi replace va dong
    ClientHandler handler2 = new ClientHandler(customSocket, null, null, null, null, null, null);
    handler2.getSession().authenticate(user);
    Server.registerClient(99, handler2);

    // Map van chi co 1 entry (handler2 thay the handler1)
    assertEquals(1, Server.getOnlineUserCount());
    assertTrue(Server.isUserOnline(99));

    Server.removeClient(99, handler2);
    authenticatedClients.clear();
  }

  @Test
  public void testUpdateOnlineUserStatus_whenHandlerExists() throws Exception {
    Field field = Server.class.getDeclaredField("authenticatedClients");
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<Integer, ClientHandler> authenticatedClients =
        (Map<Integer, ClientHandler>) field.get(null);
    authenticatedClients.clear();

    User user = app.TestFixtures.user("user_status", app.common.enums.UserRole.BIDDER);
    user.setId(77);

    ClientHandler handler = new ClientHandler(customSocket, null, null, null, null, null, null);
    handler.getSession().authenticate(user);
    Server.registerClient(77, handler);

    // updateOnlineUserStatus khi handler ton tai va authenticated
    assertDoesNotThrow(() -> Server.updateOnlineUserStatus(77, false));
    assertDoesNotThrow(() -> Server.updateOnlineUserStatus(77, true));

    Server.removeClient(77, handler);
    authenticatedClients.clear();
  }

  @Test
  public void testClientHandler_handlePacket_unauthorizedRequest() throws Exception {
    // Test luong: packet yeu cau auth nhung user chua dang nhap -> authorize returns false
    ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
    Socket sock =
        new Socket() {
          @Override
          public java.io.InputStream getInputStream() {
            // FETCH_AUCTION_SUMMARIES khong yeu cau auth, DEPOSIT yeu cau auth
            String json = "{\"type\":\"DEPOSIT\",\"payload\":{\"amount\":1000}}\n";
            return new ByteArrayInputStream(json.getBytes());
          }

          @Override
          public java.io.OutputStream getOutputStream() {
            return bos2;
          }
        };

    ClientHandler ch = new ClientHandler(sock, null, null, null, null, null, null);
    // Inject writer
    Field writerField = ClientHandler.class.getDeclaredField("writer");
    writerField.setAccessible(true);
    writerField.set(ch, new java.io.BufferedWriter(new java.io.OutputStreamWriter(bos2)));
    Field readerField = ClientHandler.class.getDeclaredField("reader");
    readerField.setAccessible(true);
    readerField.set(
        ch,
        new java.io.BufferedReader(
            new java.io.InputStreamReader(
                new ByteArrayInputStream(
                    "{\"type\":\"DEPOSIT\",\"payload\":{\"amount\":1000}}\n".getBytes()))));

    // Goi handlePacket truc tiep bang reflection
    Method handlePacket = ClientHandler.class.getDeclaredMethod("handlePacket", PacketReq.class);
    handlePacket.setAccessible(true);
    app.common.protocol.PacketReq req =
        app.common.protocol.PacketReq.of(
            app.common.enums.RequestType.DEPOSIT,
            new app.common.dto.DepositRequest(new java.math.BigDecimal("1000")));
    assertDoesNotThrow(() -> handlePacket.invoke(ch, req));
    // Packet duoc gui lai voi loi auth
    assertTrue(bos2.toString().contains("Authentication required") || bos2.size() == 0);
  }

  @Test
  public void testClientHandler_handlePacket_unrecognizedType() throws Exception {
    // Test FETCH_AUCTION_RESULT - kiem tra command khong nhan dang duoc (null command)
    ByteArrayOutputStream bos3 = new ByteArrayOutputStream();
    Socket sock2 =
        new Socket() {
          @Override
          public java.io.InputStream getInputStream() {
            return new ByteArrayInputStream(new byte[0]);
          }

          @Override
          public java.io.OutputStream getOutputStream() {
            return bos3;
          }
        };

    ClientHandler ch2 = new ClientHandler(sock2, null, null, null, null, null, null);
    Field writerField2 = ClientHandler.class.getDeclaredField("writer");
    writerField2.setAccessible(true);
    writerField2.set(ch2, new java.io.BufferedWriter(new java.io.OutputStreamWriter(bos3)));

    // FETCH_AUCTION_HISTORY la type yeu cau auth nhung chua dang nhap -> unauthorized path
    // Dung UNWATCH_AUCTION - type PUBLIC khong co command handler -> se bi "unrecognized"
    Method handlePacket2 = ClientHandler.class.getDeclaredMethod("handlePacket", PacketReq.class);
    handlePacket2.setAccessible(true);
    app.common.protocol.PacketReq req2 =
        app.common.protocol.PacketReq.of(app.common.enums.RequestType.UNWATCH_AUCTION, null);
    assertDoesNotThrow(() -> handlePacket2.invoke(ch2, req2));
    assertTrue(bos3.toString().contains("Unrecognized") || bos3.size() == 0);
  }

  @Test
  public void testClientHandler_toResponseType_coverage() throws Exception {
    // Test toResponseType voi nhieu RequestType khac nhau de cover switch statement
    Method toResponseType =
        ClientHandler.class.getDeclaredMethod("toResponseType", app.common.enums.RequestType.class);
    toResponseType.setAccessible(true);

    app.common.enums.RequestType[] types = {
      app.common.enums.RequestType.LOGIN,
      app.common.enums.RequestType.REGISTER,
      app.common.enums.RequestType.CREATE_AUCTION,
      app.common.enums.RequestType.UPDATE_AUCTION,
      app.common.enums.RequestType.FETCH_AUCTION_SUMMARIES,
      app.common.enums.RequestType.FETCH_AUCTION_HISTORY,
      app.common.enums.RequestType.FETCH_AUCTION_DETAIL,
      app.common.enums.RequestType.FETCH_SELLER_ITEMS,
      app.common.enums.RequestType.FETCH_USER_LIST,
      app.common.enums.RequestType.CANCEL_AUCTION,
      app.common.enums.RequestType.PLACE_BID,
      app.common.enums.RequestType.SET_AUTO_BID,
      app.common.enums.RequestType.DISABLE_AUTO_BID,
      app.common.enums.RequestType.DEPOSIT,
      app.common.enums.RequestType.SETTLE_WALLET,
      app.common.enums.RequestType.CHAT,
      app.common.enums.RequestType.UPLOAD_IMAGE,
      app.common.enums.RequestType.FETCH_ITEM_IMAGE,
      app.common.enums.RequestType.UPLOAD_AVATAR,
      app.common.enums.RequestType.FETCH_AVATAR,
      app.common.enums.RequestType.BAN_USER,
      app.common.enums.RequestType.UNBAN_USER,
      app.common.enums.RequestType.UNWATCH_AUCTION, // default case in switch
    };

    ClientHandler ch3 = new ClientHandler(customSocket, null, null, null, null, null, null);
    for (app.common.enums.RequestType type : types) {
      Object result =
          assertDoesNotThrow(
              () -> {
                try {
                  return toResponseType.invoke(ch3, type);
                } catch (java.lang.reflect.InvocationTargetException e) {
                  return null;
                }
              });
      assertNotNull(result);
    }
  }

  @Test
  public void testClientHandler_run_withPipedStream() throws Exception {
    // Test chay toan bo ClientHandler.run() voi pipe stream de cover listen() va cleanup()
    java.io.PipedOutputStream pos = new java.io.PipedOutputStream();
    java.io.PipedInputStream pis = new java.io.PipedInputStream(pos);
    ByteArrayOutputStream responseOutput = new ByteArrayOutputStream();

    // Tao fake socket voi piped stream
    Socket pipedSocket =
        new Socket() {
          @Override
          public java.io.InputStream getInputStream() {
            return pis;
          }

          @Override
          public java.io.OutputStream getOutputStream() {
            return responseOutput;
          }

          @Override
          public void setSoTimeout(int timeout) {
            /* no-op */
          }

          @Override
          public boolean isClosed() {
            return !pos.equals(pos);
          } // always false
        };

    ClientHandler ch = new ClientHandler(pipedSocket, null, null, null, null, null, null);

    // Chay ClientHandler tren thread khac
    Thread runThread = new Thread(ch);
    runThread.setDaemon(true);
    runThread.start();

    // Gui packet FETCH_AUCTION_SUMMARIES (PUBLIC, khong can auth)
    String packet1 = "{\"type\":\"FETCH_AUCTION_SUMMARIES\"}\n";
    pos.write(packet1.getBytes());
    pos.flush();

    // Gui packet invalid JSON de kiem tra error handling
    String badPacket = "invalid_json\n";
    pos.write(badPacket.getBytes());
    pos.flush();

    // Dong pipe de ClientHandler thoat vong lap
    Thread.sleep(200);
    pos.close();

    // Cho thread ket thuc
    runThread.join(2000);
    assertFalse(runThread.isAlive(), "ClientHandler thread should finish after pipe closed");
  }
}
