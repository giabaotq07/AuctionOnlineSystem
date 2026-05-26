package app.server.network;

import static org.junit.jupiter.api.Assertions.*;

import app.TestFixtures;
import app.common.dto.*;
import app.common.enums.*;
import app.common.models.*;
import app.common.protocol.PacketRes;
import app.server.dao.*;
import app.server.dao.impl.*;
import app.server.database.TransactionManager;
import app.server.service.*;
import app.server.service.result.AuctionCompletion;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.Socket;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.Test;

public class ServerNetworkTest extends BaseDAOTest {

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
  public void testServerStaticUtilities_withNullOrEmptyInputs() {
    // broadcastAuctionList voi null -> early return (no NPE)
    assertDoesNotThrow(() -> Server.broadcastAuctionList(null));

    // sendToUser voi null packet -> early return (no NPE)
    assertDoesNotThrow(() -> Server.sendToUser(1, null));

    // sendPacketToUser voi null packet -> early return (no NPE)
    assertDoesNotThrow(() -> Server.sendPacketToUser(1, null));

    // broadcastToAuctionViewers voi auctionId <= 0 -> early return (no NPE)
    PacketRes fakePacket = PacketRes.of(ResponseType.ERROR, "test", null);
    assertDoesNotThrow(() -> Server.broadcastToAuctionViewers(0, fakePacket, -1));
    assertDoesNotThrow(() -> Server.broadcastToAuctionViewers(-1, fakePacket, -1));

    // broadcast voi null packet -> early return (no NPE)
    assertDoesNotThrow(() -> Server.broadcast(null, -1));

    // getOnlineUserCount - khong co client nao connected
    int count = Server.getOnlineUserCount();
    assertTrue(count >= 0); // Nhu cau toi thieu: khong throw exception

    // isUserOnline - user chua dang nhap
    assertFalse(Server.isUserOnline(-999));

    // updateOnlineUserStatus voi userId khong ton tai -> early return (no NPE)
    assertDoesNotThrow(() -> Server.updateOnlineUserStatus(-999, true));

    // sendToUser voi userId khong co connected client -> early return
    assertDoesNotThrow(() -> Server.sendToUser(-999, fakePacket));
    assertDoesNotThrow(() -> Server.sendPacketToUser(-999, fakePacket));
  }

  @Test
  public void testServerPrivateMethodsWithUnsafe() throws Exception {
    // 1. Allocate Server using Unsafe to bypass port 5000 binding
    Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
    unsafeField.setAccessible(true);
    sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
    Server serverInstance = (Server) unsafe.allocateInstance(Server.class);

    // 2. Instantiate DAOs and real services using H2
    UserDAO userDAO = new MySqlUserDAO();
    ItemDAO itemDAO = new MySqlItemDAO();
    AuctionDAO auctionDAO = new MySqlAuctionDAO();
    BidDAO bidDAO = new MySqlBidDAO();
    AutoBidDAO autoBidDAO = new MySqlAutoBidDAO();
    TransactionManager transactionManager = new TransactionManager();
    BidValidator bidValidator = new BidValidator();
    Clock clock = Clock.systemDefaultZone();
    AntiSnipeService antiSnipeService = new AntiSnipeService(clock);
    AuctionSettlementService settlementService =
        new AuctionSettlementService(bidDAO, userDAO, autoBidDAO);

    UserService userService = new UserService(userDAO, transactionManager);
    ItemService itemService = new ItemService(itemDAO, auctionDAO, transactionManager);
    AutoBidService autoBidService =
        new AutoBidService(
            autoBidDAO, auctionDAO, bidDAO, itemDAO, userDAO, transactionManager, bidValidator);
    BidService bidService =
        new BidService(
            bidDAO,
            auctionDAO,
            itemDAO,
            userDAO,
            transactionManager,
            bidValidator,
            antiSnipeService,
            autoBidService);
    AuctionService auctionService =
        new AuctionService(
            auctionDAO, bidDAO, itemDAO, userDAO, transactionManager, settlementService, clock);
    AuctionQueryService auctionQueryService =
        new AuctionQueryService(auctionDAO, bidDAO, itemDAO, userDAO);
    ImageStorageService imageStorageService = new ImageStorageService();

    // 3. Set the fields on the serverInstance using reflection
    setPrivateField(serverInstance, "userService", userService);
    setPrivateField(serverInstance, "itemService", itemService);
    setPrivateField(serverInstance, "autoBidService", autoBidService);
    setPrivateField(serverInstance, "bidService", bidService);
    setPrivateField(serverInstance, "auctionService", auctionService);
    setPrivateField(serverInstance, "auctionQueryService", auctionQueryService);
    setPrivateField(serverInstance, "imageStorageService", imageStorageService);

    // 4. Test sendPaymentNotices private method
    List<AuctionCompletion> completions = new ArrayList<>();

    // Save a seller, a winner bidder, an item, and an auction in the H2 DB
    User seller =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("seller_net"), UserRole.SELLER, BigDecimal.valueOf(1000)));
    User winner =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("winner_net"), UserRole.BIDDER, BigDecimal.valueOf(5000)));
    Item item = itemDAO.save(TestFixtures.item(seller.getId(), "Test Item", ItemType.ELECTRONICS));

    Auction auction =
        TestFixtures.auction(item.getId(), seller.getId(), LocalDateTime.now().plusDays(1), 1000L);
    auction.setStatus(AuctionStatus.PAID);
    auction = auctionDAO.save(auction);

    Bid bid =
        new Bid(
            1, auction.getId(), winner.getId(), "winner_net", 1200L, LocalDateTime.now(), false);

    AuctionCompletion completion =
        new AuctionCompletion(
            auction.getId(),
            true,
            Optional.of(bid),
            BigDecimal.valueOf(1200),
            Set.of(seller.getId(), winner.getId()));
    completions.add(completion);

    // Register a fake client handler for the winner to cover sendToUser / sendPacketToUser
    FakeClientHandler fakeHandler = new FakeClientHandler();
    fakeHandler.getSession().authenticate(winner);
    Server.registerClient(winner.getId(), fakeHandler);

    // Call sendPaymentNotices
    invokePrivateMethod(
        serverInstance,
        "sendPaymentNotices",
        new Class<?>[] {List.class},
        new Object[] {completions});

    // 5. Test sendWalletUpdates
    invokePrivateMethod(
        serverInstance,
        "sendWalletUpdates",
        new Class<?>[] {List.class},
        new Object[] {completions});

    // 6. Test sendWalletUpdate
    invokePrivateMethod(
        serverInstance,
        "sendWalletUpdate",
        new Class<?>[] {int.class},
        new Object[] {winner.getId()});

    // 7. Test broadcastAuctionList
    Server.broadcastAuctionList(auctionQueryService);

    // 8. Test broadcast
    PacketRes packet = PacketRes.of(ResponseType.CHAT_RESULT, "OK", "test");
    Server.broadcast(packet, -1);

    // 9. Test broadcastToAuctionViewers
    fakeHandler.getSession().setViewingAuctionId(auction.getId());
    Server.broadcastToAuctionViewers(auction.getId(), packet, -1);

    // 10. Test updateOnlineUserStatus
    Server.updateOnlineUserStatus(winner.getId(), false);
    Server.updateOnlineUserStatus(winner.getId(), true);

    // 11. Test createAdmin
    serverInstance.createAdmin();

    // 12. Test startAuctionMaintenance scheduler path
    ScheduledExecutorService maintenancePool = Executors.newSingleThreadScheduledExecutor();
    setPrivateField(serverInstance, "auctionMaintenancePool", maintenancePool);
    invokePrivateMethod(
        serverInstance, "startAuctionMaintenance", new Class<?>[] {}, new Object[] {});
    maintenancePool.shutdownNow();

    // 13. Test shutdownExecutor directly to avoid closing shared static executors
    java.util.concurrent.ExecutorService testExecutor = Executors.newSingleThreadExecutor();
    invokePrivateMethod(
        serverInstance,
        "shutdownExecutor",
        new Class<?>[] {java.util.concurrent.ExecutorService.class, String.class},
        new Object[] {testExecutor, "testExecutor"});

    // Remove client
    Server.removeClient(winner.getId(), fakeHandler);
  }

  public static class FakeClientHandler extends ClientHandler {
    private final Session session = new Session();

    public FakeClientHandler() {
      super(null, null, null, null, null, null, null);
    }

    @Override
    public Session getSession() {
      return session;
    }

    @Override
    public boolean isAuthenticated() {
      return session.isAuthenticated();
    }

    @Override
    public User getUser() {
      return session.getUser();
    }

    @Override
    public void sendPacket(PacketRes packet) {
      // no-op
    }
  }
}
