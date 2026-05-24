package app.server.command;

import static org.junit.jupiter.api.Assertions.*;

import app.TestFixtures;
import app.common.dto.*;
import app.common.enums.*;
import app.common.models.*;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.dao.*;
import app.server.dao.impl.*;
import app.server.database.TransactionManager;
import app.server.network.ClientHandler;
import app.server.network.Session;
import app.server.service.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Lop kiem thu bo sung cho toan bo cac Command con lai phia Server. Chay tich hop tren H2 Database
 * va FakeClientHandler. Viet bang tieng Viet khong dau theo dung quy dinh.
 */
public class AdditionalCommandsTest extends BaseDAOTest {
  private FakeClientHandler fakeClientHandler;

  private UserDAO userDAO;
  private ItemDAO itemDAO;
  private AuctionDAO auctionDAO;
  private BidDAO bidDAO;
  private AutoBidDAO autoBidDAO;
  private TransactionManager transactionManager;

  private UserService userService;
  private ItemService itemService;
  private AuctionQueryService queryService;
  private BidService bidService;
  private AutoBidService autoBidService;
  private ImageStorageService imageStorageService;
  private AuctionService auctionService;

  private User seller;
  private User bidder;
  private User admin;
  private Item item;
  private Auction auction;

  @BeforeEach
  public void setUp() {
    userDAO = new MySqlUserDAO();
    itemDAO = new MySqlItemDAO();
    auctionDAO = new MySqlAuctionDAO();
    bidDAO = new MySqlBidDAO();
    autoBidDAO = new MySqlAutoBidDAO();
    transactionManager = new TransactionManager();

    userService = new UserService(userDAO, transactionManager);
    itemService = new ItemService(itemDAO, auctionDAO, transactionManager);
    queryService = new AuctionQueryService(auctionDAO, bidDAO, itemDAO, userDAO);

    BidValidator bidValidator = new BidValidator();
    imageStorageService = new ImageStorageService();
    AntiSnipeService antiSnipeService = new AntiSnipeService();
    autoBidService =
        new AutoBidService(
            autoBidDAO, auctionDAO, bidDAO, itemDAO, userDAO, transactionManager, bidValidator);
    bidService =
        new BidService(
            bidDAO,
            auctionDAO,
            itemDAO,
            userDAO,
            transactionManager,
            bidValidator,
            antiSnipeService,
            autoBidService);

    AuctionSettlementService settlementService = new AuctionSettlementService(bidDAO, userDAO);
    auctionService =
        new AuctionService(
            auctionDAO,
            bidDAO,
            itemDAO,
            userDAO,
            transactionManager,
            settlementService,
            Clock.systemDefaultZone());

    // Tao du lieu mau
    seller =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("seller_c"), UserRole.SELLER, BigDecimal.valueOf(1000)));
    bidder =
        userDAO.save(
            TestFixtures.user(
                TestFixtures.unique("bidder_c"), UserRole.BIDDER, BigDecimal.valueOf(5000)));
    admin =
        userDAO.save(
            TestFixtures.user(TestFixtures.unique("admin_c"), UserRole.ADMIN, BigDecimal.ZERO));

    item = itemDAO.save(TestFixtures.item(seller.getId(), "Painting", ItemType.ART));

    // Thiet lap startTime trong tuong lai nham thoa man dieu kien kiem tra thoi gian bat dau khi
    // update
    Auction tempAuction =
        TestFixtures.auction(item.getId(), seller.getId(), LocalDateTime.now().plusDays(1), 1000L);
    tempAuction.setStartTime(LocalDateTime.now().plusHours(1));
    auction = auctionDAO.save(tempAuction);

    fakeClientHandler = new FakeClientHandler();
  }

  @Test
  public void testCreateAuctionCommand() {
    CreateAuctionCommand cmd = new CreateAuctionCommand(auctionService, queryService);
    CreateAuctionRequest payload =
        new CreateAuctionRequest(
            "Antique Watch",
            "Old watch",
            1500L,
            100L,
            ItemType.ELECTRONICS,
            60,
            LocalDateTime.now().plusHours(1));
    PacketReq req = PacketReq.of(RequestType.CREATE_AUCTION, payload);

    fakeClientHandler.setFakeUser(seller);
    cmd.execute(fakeClientHandler, req);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertTrue(res.isSuccess());
    assertEquals(ResponseType.CREATE_AUCTION_RESULT, res.getType());
  }

  @Test
  public void testUpdateAuctionCommand() {
    UpdateAuctionCommand cmd = new UpdateAuctionCommand(auctionService, queryService);
    UpdateAuctionRequest payload =
        new UpdateAuctionRequest(
            auction.getId(),
            "Painting V2",
            "New desc",
            1200L,
            150L,
            ItemType.ART,
            120,
            LocalDateTime.now().plusHours(2),
            auction.getVersion());
    PacketReq req = PacketReq.of(RequestType.UPDATE_AUCTION, payload);

    fakeClientHandler.setFakeUser(seller);
    cmd.execute(fakeClientHandler, req);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertTrue(res.isSuccess());
    assertEquals(ResponseType.UPDATE_AUCTION_RESULT, res.getType());
  }

  @Test
  public void testFetchAuctionSummariesCommand() {
    FetchAuctionSummariesCommand cmd = new FetchAuctionSummariesCommand(queryService);
    PacketReq req = PacketReq.of(RequestType.FETCH_AUCTION_SUMMARIES, null);

    cmd.execute(fakeClientHandler, req);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertTrue(res.isSuccess());
    assertEquals(ResponseType.AUCTION_SUMMARIES_RESULT, res.getType());
  }

  @Test
  public void testFetchAuctionHistoryCommand() {
    FetchAuctionHistoryCommand cmd = new FetchAuctionHistoryCommand(queryService);
    PacketReq req = PacketReq.of(RequestType.FETCH_AUCTION_HISTORY, null);

    fakeClientHandler.setFakeUser(seller);
    cmd.execute(fakeClientHandler, req);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertTrue(res.isSuccess());
    assertEquals(ResponseType.AUCTION_HISTORY_RESULT, res.getType());
  }

  @Test
  public void testFetchAuctionDetailCommand() {
    FetchAuctionDetailCommand cmd = new FetchAuctionDetailCommand(queryService, autoBidService);
    PacketReq req =
        PacketReq.of(RequestType.FETCH_AUCTION_DETAIL, new AuctionDetailRequest(auction.getId()));

    cmd.execute(fakeClientHandler, req);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertTrue(res.isSuccess());
    assertEquals(ResponseType.AUCTION_RESULT, res.getType());
  }

  @Test
  public void testUnwatchAuctionCommand() {
    UnwatchAuctionCommand cmd = new UnwatchAuctionCommand();
    fakeClientHandler.getSession().setViewingAuctionId(10);

    PacketReq req = PacketReq.of(RequestType.UNWATCH_AUCTION, null);
    cmd.execute(fakeClientHandler, req);

    assertNull(fakeClientHandler.getSession().getViewingAuctionId());
  }

  @Test
  public void testFetchSellerItemsCommand() {
    FetchSellerItemsCommand cmd = new FetchSellerItemsCommand(itemService);
    PacketReq req =
        PacketReq.of(RequestType.FETCH_SELLER_ITEMS, new FetchSellerItemsRequest(seller.getId()));

    fakeClientHandler.setFakeUser(seller);
    cmd.execute(fakeClientHandler, req);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertTrue(res.isSuccess());
    assertEquals(ResponseType.SELLER_ITEMS_RESULT, res.getType());
  }

  @Test
  public void testFetchUserListCommand() {
    FetchUserListCommand cmd = new FetchUserListCommand(userService);
    PacketReq req = PacketReq.of(RequestType.FETCH_USER_LIST, null);

    fakeClientHandler.setFakeUser(admin);
    cmd.execute(fakeClientHandler, req);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertTrue(res.isSuccess());
    assertEquals(ResponseType.USER_LIST_RESULT, res.getType());
  }

  @Test
  public void testCancelAuctionCommand() {
    CancelAuctionCommand cmd = new CancelAuctionCommand(auctionService, queryService, userService);
    PacketReq req =
        PacketReq.of(
            RequestType.CANCEL_AUCTION,
            new CancelAuctionRequest(auction.getId(), auction.getVersion()));

    fakeClientHandler.setFakeUser(seller);
    cmd.execute(fakeClientHandler, req);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertTrue(res.isSuccess());
    assertEquals(ResponseType.CANCEL_AUCTION_RESULT, res.getType());
  }

  @Test
  public void testPlaceBidCommand() {
    PlaceBidCommand cmd = new PlaceBidCommand(bidService, userService, queryService);

    // Chuyen phien sang RUNNING de bid
    auction.start();
    auctionDAO.update(auction);

    PacketReq req =
        PacketReq.of(RequestType.PLACE_BID, new PlaceBidRequest(auction.getId(), 1500L));

    fakeClientHandler.setFakeUser(bidder);
    cmd.execute(fakeClientHandler, req);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertTrue(res.isSuccess());
    assertEquals(ResponseType.WALLET_UPDATED, res.getType());
  }

  @Test
  public void testSetAndDisableAutoBidCommands() {
    auction.start();
    auctionDAO.update(auction);

    SetAutoBidCommand setCommand = new SetAutoBidCommand(autoBidService, queryService);
    PacketReq setReq =
        PacketReq.of(RequestType.SET_AUTO_BID, new SetAutoBidRequest(auction.getId(), 2000L, 100L));

    fakeClientHandler.setFakeUser(bidder);
    setCommand.execute(fakeClientHandler, setReq);

    PacketRes setRes = fakeClientHandler.getSentPacket();
    assertNotNull(setRes);
    assertTrue(setRes.isSuccess());
    assertEquals(ResponseType.WALLET_UPDATED, setRes.getType());

    DisableAutoBidCommand disableCommand = new DisableAutoBidCommand(autoBidService);
    PacketReq disableReq =
        PacketReq.of(RequestType.DISABLE_AUTO_BID, new DisableAutoBidRequest(auction.getId()));

    disableCommand.execute(fakeClientHandler, disableReq);

    PacketRes disableRes = fakeClientHandler.getSentPacket();
    assertNotNull(disableRes);
    assertTrue(disableRes.isSuccess());
    assertEquals(ResponseType.WALLET_UPDATED, disableRes.getType());
  }

  @Test
  public void testDepositCommand() {
    DepositCommand cmd = new DepositCommand(userService);
    PacketReq req = PacketReq.of(RequestType.DEPOSIT, new DepositRequest(BigDecimal.valueOf(1000)));

    fakeClientHandler.setFakeUser(bidder);
    cmd.execute(fakeClientHandler, req);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertTrue(res.isSuccess());
    assertEquals(ResponseType.DEPOSIT_RESULT, res.getType());
  }

  @Test
  public void testSettleWalletCommand() {
    SettleWalletCommand cmd = new SettleWalletCommand(auctionService, queryService, userService);

    // Setup dau gia ket thuc va bidder thang cuoc
    auction.setStatus(AuctionStatus.FINISHED);
    auction.setWinnerId(bidder.getId());
    auction.setWinner(bidder);
    auctionDAO.update(auction);

    bidder.getWallet().setFrozenAmount(String.valueOf(auction.getId()), BigDecimal.valueOf(1000));
    userDAO.update(bidder);

    PacketReq req =
        PacketReq.of(RequestType.SETTLE_WALLET, new SettleWalletRequest(auction.getId()));

    fakeClientHandler.setFakeUser(admin);
    cmd.execute(fakeClientHandler, req);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertTrue(res.isSuccess());
    assertEquals(ResponseType.SETTLE_WALLET_RESULT, res.getType());
  }

  @Test
  public void testUploadImageCommand() throws Exception {
    UploadImageCommand cmd = new UploadImageCommand(itemService, imageStorageService, queryService);
    String base64 = java.util.Base64.getEncoder().encodeToString("dummy_image_data".getBytes());
    PacketReq req =
        PacketReq.of(
            RequestType.UPLOAD_IMAGE, new UploadImageRequest(item.getId(), base64, "avatar.png"));

    fakeClientHandler.setFakeUser(seller);
    cmd.execute(fakeClientHandler, req);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertTrue(res.isSuccess());
    assertEquals(ResponseType.UPLOAD_IMAGE, res.getType());
  }

  @Test
  public void testFetchItemImageCommand() throws Exception {
    FetchItemImageCommand cmd = new FetchItemImageCommand(imageStorageService, itemService);
    String base64 = java.util.Base64.getEncoder().encodeToString("dummy_image_data".getBytes());
    String path = imageStorageService.save(base64, "image.jpg");

    item.setImageUrl(path);
    itemService.update(item);

    PacketReq req =
        PacketReq.of(RequestType.FETCH_ITEM_IMAGE, new FetchItemImageRequest(item.getId()));

    cmd.execute(fakeClientHandler, req);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertTrue(res.isSuccess());
    assertEquals(ResponseType.FETCH_ITEM_IMAGE, res.getType());

    imageStorageService.deleteIfExists(path);
  }

  @Test
  public void testChatCommand() {
    ChatCommand cmd = new ChatCommand();
    PacketReq req = PacketReq.of(RequestType.CHAT, new ChatRequest("Hello World"));

    fakeClientHandler.setFakeUser(bidder);
    cmd.execute(fakeClientHandler, req);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertTrue(res.isSuccess());
    assertEquals(ResponseType.CHAT_MESSAGE, res.getType());
  }

  /** FakeClientHandler de gia lap socket connection va session dang nhap. */
  public static class FakeClientHandler extends ClientHandler {
    private final Session fakeSession = new Session();
    private PacketRes sentPacket;

    public FakeClientHandler() {
      super(null, null, null, null, null, null, null);
    }

    public void setFakeUser(User user) {
      fakeSession.authenticate(user);
    }

    @Override
    public Session getSession() {
      return fakeSession;
    }

    @Override
    public boolean isAuthenticated() {
      return fakeSession.isAuthenticated();
    }

    @Override
    public User getUser() {
      return fakeSession.getUser();
    }

    @Override
    public void sendPacket(PacketRes packet) {
      this.sentPacket = packet;
    }

    public PacketRes getSentPacket() {
      return sentPacket;
    }
  }
}
