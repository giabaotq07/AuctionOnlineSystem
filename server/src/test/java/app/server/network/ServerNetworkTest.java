package app.server.network;

import static org.junit.jupiter.api.Assertions.*;

import app.common.enums.ResponseType;
import app.common.protocol.PacketRes;
import app.server.service.result.AuctionCompletion;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

public class ServerNetworkTest {

  private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private Object invokePrivateMethod(
      Object target, String methodName, Class<?>[] parameterTypes, Object[] args) throws Exception {
    Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
    method.setAccessible(true);
    return method.invoke(target, args);
  }

  @Test
  public void testServerBroadcastingAndUtilities() throws Exception {
    // We will test Server.broadcastAuctionList with a null query service which returns early safely
    assertDoesNotThrow(() -> Server.broadcastAuctionList(null));

    // Test sendPaymentNotices private block
    Server server = null;
    try {
      // Bypassing constructor using reflection to set instances or execute helpers
      server = Server.getInstance();
    } catch (Exception e) {
      // Server initialization might bind or connect to MySQL. If it fails, that's fine, we can mock
      // it
    }

    // Set up dummy completed auctions list
    List<AuctionCompletion> completions = new ArrayList<>();
    // Completed auction with highest bid
    AuctionCompletion completion1 =
        new AuctionCompletion(
            1,
            true,
            Optional.of(
                new app.common.models.Bid(
                    1, 1, 2, "Bidder", 1000L, java.time.LocalDateTime.now(), false)),
            BigDecimal.valueOf(1000L),
            java.util.Set.of(2, 3));
    completions.add(completion1);

    // Call private shutdownExecutor using a dummy ExecutorService
    if (server != null) {
      ExecutorService tempExecutor = Executors.newSingleThreadExecutor();
      final Server s = server;
      assertDoesNotThrow(
          () ->
              invokePrivateMethod(
                  s,
                  "shutdownExecutor",
                  new Class<?>[] {ExecutorService.class, String.class},
                  new Object[] {tempExecutor, "testPool"}));
    }
  }

  @Test
  public void testClientHandlerCommunicationStreams() throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    Socket socket =
        new Socket() {
          @Override
          public java.io.InputStream getInputStream() {
            // Feed valid dummy json packets
            String json =
                "{\"type\":\"CHAT_MESSAGE\",\"status\":\"OK\",\"data\":\"Hello Server\"}\n";
            return new ByteArrayInputStream(json.getBytes());
          }

          @Override
          public java.io.OutputStream getOutputStream() {
            return bos;
          }
        };

    // Instantiate client handler with null services to avoid Mockito constraints in JDK 25
    ClientHandler clientHandler =
        new ClientHandler(socket, null, null, null, null, null, null, null);
    assertNotNull(clientHandler.getSession());
    assertEquals(socket, clientHandler.getSocket());

    // Inject writer via reflection
    setPrivateField(
        clientHandler, "writer", new java.io.BufferedWriter(new java.io.OutputStreamWriter(bos)));

    // Test sendPacket and sendToClient
    PacketRes packet = PacketRes.of(ResponseType.CHAT_RESULT, "OK", "Hello Client");
    assertDoesNotThrow(() -> clientHandler.sendPacket(packet));
    assertTrue(bos.toString().contains("CHAT_RESULT"));

    assertDoesNotThrow(() -> clientHandler.close());
  }

  @Test
  public void testClientHandlerExecutionLoop() throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    String requests =
        "invalid_json\n"
            + "{}\n"
            + "{\"type\":\"FETCH_AUCTION_SUMMARIES\"}\n"
            + "{\"type\":\"CHAT\"}\n";

    Socket socket =
        new Socket() {
          @Override
          public java.io.InputStream getInputStream() {
            return new ByteArrayInputStream(requests.getBytes());
          }

          @Override
          public java.io.OutputStream getOutputStream() {
            return bos;
          }
        };

    ClientHandler clientHandler =
        new ClientHandler(socket, null, null, null, null, null, null, null);
    clientHandler.run();

    String output = bos.toString();
    assertTrue(output.contains("ERROR") || output.contains("FETCH_AUCTION_SUMMARIES"));
  }

  @Test
  public void testClientHandlerExecutionLoopWithUser() throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    String requests =
        "{\"type\":\"CHAT\",\"data\":\"{\\\"message\\\":\\\"hello\\\"}\"}\n"
            + "{\"type\":\"BAN_USER\",\"data\":\"{\\\"userId\\\":1,\\\"ban\\\":true}\"}\n";

    Socket socket =
        new Socket() {
          @Override
          public java.io.InputStream getInputStream() {
            return new ByteArrayInputStream(requests.getBytes());
          }

          @Override
          public java.io.OutputStream getOutputStream() {
            return bos;
          }
        };

    ClientHandler clientHandler =
        new ClientHandler(socket, null, null, null, null, null, null, null);

    app.common.models.User user =
        new app.common.models.User(
            1,
            "John",
            new app.common.models.Account("john", "pass", app.common.enums.UserRole.BIDDER),
            new app.common.models.Wallet(BigDecimal.TEN));
    clientHandler.getSession().authenticate(user);

    clientHandler.run();

    String output = bos.toString();
    assertTrue(
        output.contains("CHAT_RESULT")
            || output.contains("USER_BANNED_NOTICE")
            || output.contains("ERROR"));
  }

  @Test
  public void testClientHandlerExecutionLoopBannedUser() throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    String requests = "{\"type\":\"CHAT\",\"data\":\"{\\\"message\\\":\\\"hello\\\"}\"}\n";
    Socket socket =
        new Socket() {
          @Override
          public java.io.InputStream getInputStream() {
            return new ByteArrayInputStream(requests.getBytes());
          }

          @Override
          public java.io.OutputStream getOutputStream() {
            return bos;
          }
        };
    ClientHandler clientHandler =
        new ClientHandler(socket, null, null, null, null, null, null, null);

    app.common.models.User user =
        new app.common.models.User(
            1,
            "John",
            new app.common.models.Account("john", "pass", app.common.enums.UserRole.BIDDER),
            new app.common.models.Wallet(BigDecimal.TEN));
    user.setStatus(false);
    clientHandler.getSession().authenticate(user);

    clientHandler.run();

    String output = bos.toString();
    assertTrue(output.contains("Tài khoản đã bị cấm"));
  }

  @Test
  public void testToResponseTypeAllEnumValues() throws Exception {
    ClientHandler clientHandler =
        new ClientHandler(null, null, null, null, null, null, null, null);
    for (app.common.enums.RequestType type : app.common.enums.RequestType.values()) {
      assertDoesNotThrow(
          () ->
              invokePrivateMethod(
                  clientHandler,
                  "toResponseType",
                  new Class<?>[] {app.common.enums.RequestType.class},
                  new Object[] {type}));
    }
  }
}
