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
  public void testBanUserCommandBanAndUnban() {
    BanUserCommand cmd = new BanUserCommand(userService);
    fakeClientHandler.setFakeUser(admin);

    cmd.execute(
        fakeClientHandler,
        PacketReq.of(RequestType.BAN_USER, new BanUserRequest(bidder.getId(), true)));

    PacketRes banRes = fakeClientHandler.getSentPacket();
    assertNotNull(banRes);
    assertTrue(banRes.isSuccess());
    assertEquals(ResponseType.USER_BANNED_NOTICE, banRes.getType());
    assertTrue(banRes.getData(BanUserResponse.class).isBanned());
    assertTrue(userDAO.findById(bidder.getId()).orElseThrow().isBanned());

    cmd.execute(
        fakeClientHandler,
        PacketReq.of(RequestType.UNBAN_USER, new BanUserRequest(bidder.getId(), false)));

    PacketRes unbanRes = fakeClientHandler.getSentPacket();
    assertNotNull(unbanRes);
    assertTrue(unbanRes.isSuccess());
    assertFalse(unbanRes.getData(BanUserResponse.class).isBanned());
    assertFalse(userDAO.findById(bidder.getId()).orElseThrow().isBanned());
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

  @Test
  public void testFetchAuctionResultCommand() {
    FetchAuctionResultCommand cmd = new FetchAuctionResultCommand(auctionService, userService);

    // Test that bidding finished results can be fetched
    auction.setStatus(AuctionStatus.FINISHED);
    auctionDAO.update(auction);

    PacketReq req =
        PacketReq.of(RequestType.FETCH_AUCTION_DETAIL, new AuctionResultRequest(auction.getId()));
    cmd.execute(fakeClientHandler, req);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertTrue(res.isSuccess());
    assertEquals(ResponseType.AUCTION_RESULT_FETCHED, res.getType());
  }

  @Test
  public void testFetchAvatarCommand() throws Exception {
    FetchAvatarCommand cmd = new FetchAvatarCommand(imageStorageService);

    // Save dummy avatar
    String base64 = java.util.Base64.getEncoder().encodeToString("avatar_data".getBytes());
    String path = imageStorageService.save(base64, "avatar.png");

    PacketReq req =
        PacketReq.of(RequestType.FETCH_AVATAR, new FetchAvatarRequest(bidder.getId(), path));
    cmd.execute(fakeClientHandler, req);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertTrue(res.isSuccess());
    assertEquals(ResponseType.FETCH_AVATAR, res.getType());

    imageStorageService.deleteIfExists(path);
  }

  @Test
  public void testUploadAvatarCommand() throws Exception {
    UploadAvatarCommand cmd = new UploadAvatarCommand(userService, imageStorageService);
    String base64 = java.util.Base64.getEncoder().encodeToString("new_avatar_data".getBytes());

    PacketReq req =
        PacketReq.of(RequestType.UPLOAD_AVATAR, new UploadAvatarRequest(base64, "new_avatar.png"));

    fakeClientHandler.setFakeUser(bidder);
    cmd.execute(fakeClientHandler, req);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertTrue(res.isSuccess());
    assertEquals(ResponseType.UPLOAD_AVATAR, res.getType());
    assertNotNull(bidder.getAvatarUrl());

    imageStorageService.deleteIfExists(bidder.getAvatarUrl());
  }

  static class ExceptionCommand extends SafeCommand {
    private final Exception exceptionToThrow;

    public ExceptionCommand(Exception exceptionToThrow) {
      this.exceptionToThrow = exceptionToThrow;
    }

    @Override
    protected void doExecute(ClientHandler clientHandler, PacketReq packet) throws Exception {
      throw exceptionToThrow;
    }

    @Override
    protected ResponseType responseType() {
      return ResponseType.ERROR;
    }
  }

  @Test
  public void testSafeCommandExceptions() {
    // 1. ServiceException
    ExceptionCommand serviceCmd =
        new ExceptionCommand(new app.common.exception.ServiceException("Service error"));
    serviceCmd.execute(fakeClientHandler, PacketReq.of(RequestType.CHAT, null));
    assertEquals("Service error", fakeClientHandler.getSentPacket().getMessage());

    // 2. DatabaseException
    ExceptionCommand dbCmd =
        new ExceptionCommand(new app.common.exception.DatabaseException("DB error"));
    dbCmd.execute(fakeClientHandler, PacketReq.of(RequestType.CHAT, null));
    assertEquals(dbCmd.databaseErrorMessage(), fakeClientHandler.getSentPacket().getMessage());

    // 3. JsonSyntaxException
    ExceptionCommand jsonCmd =
        new ExceptionCommand(new com.google.gson.JsonSyntaxException("JSON syntax"));
    jsonCmd.execute(fakeClientHandler, PacketReq.of(RequestType.CHAT, null));
    assertEquals(jsonCmd.invalidRequestMessage(), fakeClientHandler.getSentPacket().getMessage());

    // 4. IllegalArgumentException
    ExceptionCommand illegalCmd =
        new ExceptionCommand(new IllegalArgumentException("Invalid argument"));
    illegalCmd.execute(fakeClientHandler, PacketReq.of(RequestType.CHAT, null));
    assertEquals("Invalid argument", fakeClientHandler.getSentPacket().getMessage());

    // 5. IOException
    ExceptionCommand ioCmd = new ExceptionCommand(new java.io.IOException("IO failed"));
    ioCmd.execute(fakeClientHandler, PacketReq.of(RequestType.CHAT, null));
    assertEquals(ioCmd.ioErrorMessage(), fakeClientHandler.getSentPacket().getMessage());

    // 6. Generic Exception
    ExceptionCommand genericCmd = new ExceptionCommand(new RuntimeException("Unexpected error"));
    genericCmd.execute(fakeClientHandler, PacketReq.of(RequestType.CHAT, null));
    assertEquals(
        genericCmd.unexpectedErrorMessage(), fakeClientHandler.getSentPacket().getMessage());
  }

  @Test
  public void testCommandsValidationFailures() {
    // 1. CreateAuctionCommand validation
    CreateAuctionCommand createCmd = new CreateAuctionCommand(auctionService, queryService);
    createCmd.execute(fakeClientHandler, PacketReq.of(RequestType.CREATE_AUCTION, null));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // 2. UpdateAuctionCommand validation
    UpdateAuctionCommand updateCmd = new UpdateAuctionCommand(auctionService, queryService);
    updateCmd.execute(fakeClientHandler, PacketReq.of(RequestType.UPDATE_AUCTION, null));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // 3. FetchSellerItemsCommand validation
    FetchSellerItemsCommand sellerItemsCmd = new FetchSellerItemsCommand(itemService);
    sellerItemsCmd.execute(fakeClientHandler, PacketReq.of(RequestType.FETCH_SELLER_ITEMS, null));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // 4. BanUserCommand validation
    BanUserCommand banCmd = new BanUserCommand(userService);
    banCmd.execute(fakeClientHandler, PacketReq.of(RequestType.BAN_USER, null));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // 5. CancelAuctionCommand validation
    CancelAuctionCommand cancelCmd = new CancelAuctionCommand(auctionService, queryService, userService);
    cancelCmd.execute(fakeClientHandler, PacketReq.of(RequestType.CANCEL_AUCTION, null));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // 6. PlaceBidCommand validation
    PlaceBidCommand bidCmd = new PlaceBidCommand(bidService, userService, queryService);
    bidCmd.execute(fakeClientHandler, PacketReq.of(RequestType.PLACE_BID, null));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // 7. SetAutoBidCommand validation
    SetAutoBidCommand autoBidCmd = new SetAutoBidCommand(autoBidService, queryService);
    autoBidCmd.execute(fakeClientHandler, PacketReq.of(RequestType.SET_AUTO_BID, null));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // 8. DisableAutoBidCommand validation
    DisableAutoBidCommand disableAutoBidCmd = new DisableAutoBidCommand(autoBidService);
    disableAutoBidCmd.execute(fakeClientHandler, PacketReq.of(RequestType.DISABLE_AUTO_BID, null));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // 9. DepositCommand validation
    DepositCommand depositCmd = new DepositCommand(userService);
    depositCmd.execute(fakeClientHandler, PacketReq.of(RequestType.DEPOSIT, null));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // 10. SettleWalletCommand validation
    SettleWalletCommand settleCmd = new SettleWalletCommand(auctionService, queryService, userService);
    settleCmd.execute(fakeClientHandler, PacketReq.of(RequestType.SETTLE_WALLET, null));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // 11. UploadImageCommand validation
    UploadImageCommand uploadImageCmd = new UploadImageCommand(itemService, imageStorageService, queryService);
    uploadImageCmd.execute(fakeClientHandler, PacketReq.of(RequestType.UPLOAD_IMAGE, null));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // 12. FetchItemImageCommand validation
    FetchItemImageCommand fetchImageCmd = new FetchItemImageCommand(imageStorageService, itemService);
    fetchImageCmd.execute(fakeClientHandler, PacketReq.of(RequestType.FETCH_ITEM_IMAGE, null));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // 13. FetchAvatarCommand validation
    FetchAvatarCommand fetchAvatarCmd = new FetchAvatarCommand(imageStorageService);
    fetchAvatarCmd.execute(fakeClientHandler, PacketReq.of(RequestType.FETCH_AVATAR, null));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // 14. UploadAvatarCommand validation
    UploadAvatarCommand uploadAvatarCmd = new UploadAvatarCommand(userService, imageStorageService);
    uploadAvatarCmd.execute(fakeClientHandler, PacketReq.of(RequestType.UPLOAD_AVATAR, null));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());
  }

  @Test
  public void testCommandsRequireUserFailures() {
    FakeClientHandler guestHandler = new FakeClientHandler(); // No user authenticated

    // 1. CreateAuctionCommand requires logged in user
    CreateAuctionCommand createCmd = new CreateAuctionCommand(auctionService, queryService);
    CreateAuctionRequest createPayload =
        new CreateAuctionRequest(
            "Name",
            "Desc",
            1000L,
            100L,
            ItemType.ART,
            60,
            LocalDateTime.now().plusDays(1));
    createCmd.execute(guestHandler, PacketReq.of(RequestType.CREATE_AUCTION, createPayload));
    assertFalse(guestHandler.getSentPacket().isSuccess());
    assertEquals(
        "Người dùng chưa đăng nhập hoặc không hợp lệ.",
        guestHandler.getSentPacket().getMessage());

    // 2. PlaceBidCommand requires logged in user
    PlaceBidCommand bidCmd = new PlaceBidCommand(bidService, userService, queryService);
    PlaceBidRequest bidPayload = new PlaceBidRequest(1, 1500L);
    bidCmd.execute(guestHandler, PacketReq.of(RequestType.PLACE_BID, bidPayload));
    assertFalse(guestHandler.getSentPacket().isSuccess());
  }

  @Test
  public void testRegisterCommandValidationFailures() {
    RegisterCommand regCmd = new RegisterCommand(userService);

    // Empty fields
    regCmd.execute(fakeClientHandler, PacketReq.of(RequestType.REGISTER, new RegisterRequest("", "username", "password", UserRole.BIDDER)));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    regCmd.execute(fakeClientHandler, PacketReq.of(RequestType.REGISTER, new RegisterRequest("Name", "", "password", UserRole.BIDDER)));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    regCmd.execute(fakeClientHandler, PacketReq.of(RequestType.REGISTER, new RegisterRequest("Name", "username", "  ", UserRole.BIDDER)));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    regCmd.execute(fakeClientHandler, PacketReq.of(RequestType.REGISTER, new RegisterRequest(null, "username", "password", UserRole.BIDDER)));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // Invalid role
    regCmd.execute(fakeClientHandler, PacketReq.of(RequestType.REGISTER, new RegisterRequest("Name", "username", "password", UserRole.ADMIN)));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());
    assertEquals("Vai trò không hợp lệ.", fakeClientHandler.getSentPacket().getMessage());

    // Null role defaults to BIDDER
    regCmd.execute(fakeClientHandler, PacketReq.of(RequestType.REGISTER, new RegisterRequest("Name " + TestFixtures.unique("n"), "user_" + TestFixtures.unique("u"), "password", null)));
    assertTrue(fakeClientHandler.getSentPacket().isSuccess());
  }

  @Test
  public void testFetchAuctionDetailCommandAdditional() {
    FetchAuctionDetailCommand cmd = new FetchAuctionDetailCommand(queryService, autoBidService);

    // 1. Invalid auction ID <= 0
    cmd.execute(fakeClientHandler, PacketReq.of(RequestType.FETCH_AUCTION_DETAIL, new AuctionDetailRequest(0)));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());
    assertEquals("Phiên đấu giá không hợp lệ.", fakeClientHandler.getSentPacket().getMessage());

    // 2. knownVersion matches current auction version
    int currentVer = auction.getVersion();
    cmd.execute(fakeClientHandler, PacketReq.of(RequestType.FETCH_AUCTION_DETAIL, new AuctionDetailRequest(auction.getId(), currentVer)));
    assertTrue(fakeClientHandler.getSentPacket().isSuccess());
    assertEquals(ResponseType.AUCTION_RESULT, fakeClientHandler.getSentPacket().getType());

    // 3. Authenticated user with no auto-bid
    fakeClientHandler.setFakeUser(bidder);
    cmd.execute(fakeClientHandler, PacketReq.of(RequestType.FETCH_AUCTION_DETAIL, new AuctionDetailRequest(auction.getId())));
    assertTrue(fakeClientHandler.getSentPacket().isSuccess());

    // 4. Authenticated user with auto-bid configured
    AutoBid autoBid = new AutoBid();
    autoBid.setAuctionId(auction.getId());
    autoBid.setUserId(bidder.getId());
    autoBid.setMaxAmount(3000L);
    autoBid.setIncrementAmount(200L);
    autoBid.setEnabled(true);
    AutoBid savedAutoBid = autoBidDAO.save(autoBid);
    cmd.execute(fakeClientHandler, PacketReq.of(RequestType.FETCH_AUCTION_DETAIL, new AuctionDetailRequest(auction.getId())));
    assertTrue(fakeClientHandler.getSentPacket().isSuccess());
    autoBidDAO.delete(savedAutoBid.getId()); // cleanup

    // 5. Throws exception inside autoBidService
    AutoBidService throwingAutoBidService = new AutoBidService(autoBidDAO, auctionDAO, bidDAO, itemDAO, userDAO, transactionManager, new BidValidator()) {
      @Override
      public java.util.Optional<AutoBid> getAutoBid(int aId, int uId) {
        throw new RuntimeException("Simulated AutoBidService error");
      }
    };
    FetchAuctionDetailCommand throwingCmd = new FetchAuctionDetailCommand(queryService, throwingAutoBidService);
    assertDoesNotThrow(() -> throwingCmd.execute(fakeClientHandler, PacketReq.of(RequestType.FETCH_AUCTION_DETAIL, new AuctionDetailRequest(auction.getId()))));
  }

  @Test
  public void testSettleWalletCommandAdditional() {
    SettleWalletCommand cmd = new SettleWalletCommand(auctionService, queryService, userService);

    // 1. Invalid auction ID <= 0
    cmd.execute(fakeClientHandler, PacketReq.of(RequestType.SETTLE_WALLET, new SettleWalletRequest(0)));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // 2. requireUser validation failure
    FakeClientHandler guestHandler = new FakeClientHandler();
    cmd.execute(guestHandler, PacketReq.of(RequestType.SETTLE_WALLET, new SettleWalletRequest(auction.getId())));
    assertFalse(guestHandler.getSentPacket().isSuccess());

    // 3. Settle completed auction (expired)
    // Setup expired auction
    Item expiredItem1 = itemDAO.save(TestFixtures.item(seller.getId(), "Painting Expired 1", ItemType.ART));
    Auction expiredAuction = TestFixtures.auction(expiredItem1.getId(), seller.getId(), LocalDateTime.now().minusHours(1), 1000L);
    expiredAuction.setStartTime(LocalDateTime.now().minusHours(2));
    expiredAuction.setStatus(AuctionStatus.RUNNING);
    expiredAuction = auctionDAO.save(expiredAuction);

    bidder.getWallet().setFrozenAmount(String.valueOf(expiredAuction.getId()), BigDecimal.valueOf(1000));
    userDAO.update(bidder);

    // Register clients for notification
    fakeClientHandler.setFakeUser(admin);
    app.server.network.Server.registerClient(seller.getId(), fakeClientHandler);
    app.server.network.Server.registerClient(bidder.getId(), fakeClientHandler);

    cmd.execute(fakeClientHandler, PacketReq.of(RequestType.SETTLE_WALLET, new SettleWalletRequest(expiredAuction.getId())));
    assertTrue(fakeClientHandler.getSentPacket().isSuccess());

    // 4. Exception case on broadcast / sendToUser
    // Register throwing handlers
    FakeClientHandler throwingHandler = new FakeClientHandler() {
      @Override
      public void sendPacket(PacketRes packet) {
        throw new RuntimeException("Simulated notification send failure");
      }
    };
    app.server.network.Server.registerClient(seller.getId(), throwingHandler);
    app.server.network.Server.registerClient(bidder.getId(), throwingHandler);

    // Setup another expired auction
    Item expiredItem2 = itemDAO.save(TestFixtures.item(seller.getId(), "Painting Expired 2", ItemType.ART));
    Auction tempExpiredAuction2 = TestFixtures.auction(expiredItem2.getId(), seller.getId(), LocalDateTime.now().minusHours(1), 1000L);
    tempExpiredAuction2.setStartTime(LocalDateTime.now().minusHours(2));
    tempExpiredAuction2.setStatus(AuctionStatus.RUNNING);
    final Auction savedExpiredAuction2 = auctionDAO.save(tempExpiredAuction2);

    bidder.getWallet().setFrozenAmount(String.valueOf(savedExpiredAuction2.getId()), BigDecimal.valueOf(1000));
    userDAO.update(bidder);

    assertDoesNotThrow(() -> cmd.execute(fakeClientHandler, PacketReq.of(RequestType.SETTLE_WALLET, new SettleWalletRequest(savedExpiredAuction2.getId()))));
  }

  @Test
  public void testCancelAuctionCommandAdditional() {
    CancelAuctionCommand cmd = new CancelAuctionCommand(auctionService, queryService, userService);

    // 1. Invalid auction ID / expectedVersion
    cmd.execute(fakeClientHandler, PacketReq.of(RequestType.CANCEL_AUCTION, new CancelAuctionRequest(0, 1)));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    cmd.execute(fakeClientHandler, PacketReq.of(RequestType.CANCEL_AUCTION, new CancelAuctionRequest(1, -1)));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // 2. Exception coverage in notifications / sendWalletUpdates
    // Create new running auction to cancel
    Item cancelItem = itemDAO.save(TestFixtures.item(seller.getId(), "Painting to Cancel", ItemType.ART));
    Auction cancelAuction = TestFixtures.auction(cancelItem.getId(), seller.getId(), LocalDateTime.now().plusHours(2), 1000L);
    cancelAuction.setStatus(AuctionStatus.RUNNING);
    cancelAuction = auctionDAO.save(cancelAuction);

    // Setup a bid and auto-bid so that we have releasedUserIds
    fakeClientHandler.setFakeUser(seller);
    
    // Register a throwing handler to hit catch block in sendWalletUpdates
    FakeClientHandler throwingHandler = new FakeClientHandler() {
      @Override
      public void sendPacket(PacketRes packet) {
        throw new RuntimeException("Simulated notify cancel failure");
      }
    };
    app.server.network.Server.registerClient(bidder.getId(), throwingHandler);

    // Set bidder wallet frozen amount so cancel releases it and adds to releasedUserIds
    bidder.getWallet().setFrozenAmount(String.valueOf(cancelAuction.getId()), BigDecimal.valueOf(500));
    userDAO.update(bidder);

    cmd.execute(fakeClientHandler, PacketReq.of(RequestType.CANCEL_AUCTION, new CancelAuctionRequest(cancelAuction.getId(), cancelAuction.getVersion())));
    assertTrue(fakeClientHandler.getSentPacket().isSuccess());
  }

  @Test
  public void testUploadAvatarCommandAdditional() {
    UploadAvatarCommand cmd = new UploadAvatarCommand(userService, imageStorageService);

    // 1. Null data
    cmd.execute(fakeClientHandler, PacketReq.of(RequestType.UPLOAD_AVATAR, new UploadAvatarRequest(null, "avatar.png")));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    cmd.execute(fakeClientHandler, PacketReq.of(RequestType.UPLOAD_AVATAR, new UploadAvatarRequest("base64", null)));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // 2. Guest user
    FakeClientHandler guestHandler = new FakeClientHandler();
    cmd.execute(guestHandler, PacketReq.of(RequestType.UPLOAD_AVATAR, new UploadAvatarRequest("base64", "avatar.png")));
    assertFalse(guestHandler.getSentPacket().isSuccess());

    // 3. Exception throwers - IOException, IllegalArgumentException, ServiceException
    fakeClientHandler.setFakeUser(bidder);

    // IllegalArgumentException (e.g. invalid base64 format)
    cmd.execute(fakeClientHandler, PacketReq.of(RequestType.UPLOAD_AVATAR, new UploadAvatarRequest("invalid_base64_&^*()", "avatar.png")));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // ServiceException / IOException simulation using custom throwing ImageStorageService
    ImageStorageService throwingStorage = new ImageStorageService() {
      @Override
      public String save(String b64, String filename) throws java.io.IOException {
        throw new java.io.IOException("Simulated disk write error");
      }
    };
    UploadAvatarCommand cmdThrowing = new UploadAvatarCommand(userService, throwingStorage);
    cmdThrowing.execute(fakeClientHandler, PacketReq.of(RequestType.UPLOAD_AVATAR, new UploadAvatarRequest("YmFzZTY0", "avatar.png")));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());
  }

  @Test
  public void testFetchAvatarCommandAdditional() {
    FetchAvatarCommand cmd = new FetchAvatarCommand(imageStorageService);

    // 1. Invalid path
    cmd.execute(fakeClientHandler, PacketReq.of(RequestType.FETCH_AVATAR, new FetchAvatarRequest(bidder.getId(), null)));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    cmd.execute(fakeClientHandler, PacketReq.of(RequestType.FETCH_AVATAR, new FetchAvatarRequest(bidder.getId(), "  ")));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // 2. IOException / path not exists
    cmd.execute(fakeClientHandler, PacketReq.of(RequestType.FETCH_AVATAR, new FetchAvatarRequest(bidder.getId(), "nonexistent_avatar.png")));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());
  }

  @Test
  public void testUploadImageCommandAdditional() {
    UploadImageCommand cmd = new UploadImageCommand(itemService, imageStorageService, queryService);

    // 1. Validation failure (invalid inputs)
    cmd.execute(fakeClientHandler, PacketReq.of(RequestType.UPLOAD_IMAGE, new UploadImageRequest(0, "base64", "img.png")));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    cmd.execute(fakeClientHandler, PacketReq.of(RequestType.UPLOAD_IMAGE, new UploadImageRequest(item.getId(), null, "img.png")));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    cmd.execute(fakeClientHandler, PacketReq.of(RequestType.UPLOAD_IMAGE, new UploadImageRequest(item.getId(), "base64", null)));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // 2. Cleanup deletion on failure
    // We will simulate updateImagePath throwing ServiceException, so it cleans up the saved file
    ItemService throwingItemService = new ItemService(itemDAO, auctionDAO, transactionManager) {
      @Override
      public String updateImagePath(int itemId, String path, int userId, UserRole role) {
        throw new app.common.exception.ServiceException("Simulated DB update failure");
      }
    };
    UploadImageCommand cleanupCmd = new UploadImageCommand(throwingItemService, imageStorageService, queryService);
    fakeClientHandler.setFakeUser(seller);
    String base64 = java.util.Base64.getEncoder().encodeToString("dummy_image_data".getBytes());
    
    cleanupCmd.execute(fakeClientHandler, PacketReq.of(RequestType.UPLOAD_IMAGE, new UploadImageRequest(item.getId(), base64, "avatar.png")));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());
  }

  @Test
  public void testSafeCommandHelperEdgeCases() {
    SafeCommand cmd = new SafeCommand() {
      @Override
      protected void doExecute(ClientHandler clientHandler, PacketReq packet) {
        // Test requireUser with id <= 0
        User invalidUser = new User("Name", new Account("username", "password", UserRole.BIDDER), new Wallet());
        invalidUser.setId(0);
        FakeClientHandler invalidHandler = new FakeClientHandler();
        invalidHandler.setFakeUser(invalidUser);
        requireUser(invalidHandler);
      }
      @Override
      protected ResponseType responseType() {
        return ResponseType.ERROR;
      }
    };

    // requireUser with id <= 0 will throw ValidationException which is caught in execute
    cmd.execute(fakeClientHandler, PacketReq.of(RequestType.CHAT, null));
    assertFalse(fakeClientHandler.getSentPacket().isSuccess());

    // Test sendError with null clientHandler
    assertDoesNotThrow(() -> cmd.sendError(null, "Error message"));
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
