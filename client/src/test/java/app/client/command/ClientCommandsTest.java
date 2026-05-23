package app.client.command;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.client.manager.ClientNotificationCenter;
import app.client.utils.AlertUtils;
import app.common.dto.*;
import app.common.enums.*;
import app.common.protocol.PacketRes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javafx.application.Platform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * ClientCommandsTest. Kiem thu tat ca cac lop Command o phia Client. Khoi dong JavaFX Platform thuc
 * te va dung AlertUtils mock de tuong thich JDK 25.
 */
public class ClientCommandsTest {

  private MockedStatic<AlertUtils> mockedAlertUtils;
  private boolean messageNotified = false;
  private boolean updateNotified = false;
  private ChatResponse chatNotified = null;

  @BeforeEach
  void setUp() {
    // Khoi dong JavaFX Platform thuc te va giu cho no luon chay bang cach setImplicitExit(false)
    try {
      Platform.startup(() -> {});
      Platform.setImplicitExit(false);
    } catch (IllegalStateException e) {
      // Da khoi dong tu truoc
    }

    mockedAlertUtils = mockStatic(AlertUtils.class);

    // Lang nghe cac notification tu ClientNotificationCenter
    ClientNotificationCenter.getInstance().addMessageListener(msg -> messageNotified = true);
    ClientNotificationCenter.getInstance().addUpdateListener(() -> updateNotified = true);
    ClientNotificationCenter.getInstance().addChatListener(chat -> chatNotified = chat);

    messageNotified = false;
    updateNotified = false;
    chatNotified = null;
  }

  @AfterEach
  void tearDown() {
    mockedAlertUtils.close();
  }

  @Test
  public void testCommandBaseMethods() {
    Command baseCmd =
        new Command() {
          @Override
          public void execute(PacketRes packet) {}
        };

    baseCmd.notifyMessage("Test Message");
    assertTrue(messageNotified);

    baseCmd.notifyUpdate();
    assertTrue(updateNotified);

    ChatResponse dummyChat = new ChatResponse(1, "UserA", "Hello", LocalDateTime.now());
    baseCmd.notifyChat(dummyChat);
    assertEquals(dummyChat, chatNotified);
  }

  @Test
  public void testCancelAuctionCommand() {
    CancelAuctionCommand cmd = new CancelAuctionCommand();

    // Success case
    CancelAuctionResponse payload = new CancelAuctionResponse(123);
    PacketRes successPacket = PacketRes.of(ResponseType.CANCEL_AUCTION_RESULT, "Success", payload);
    cmd.execute(successPacket);
    assertTrue(updateNotified);

    // Failure case
    PacketRes failPacket = PacketRes.error(ResponseType.CANCEL_AUCTION_RESULT, "Error");
    cmd.execute(failPacket);
  }

  @Test
  public void testDepositCommand() {
    DepositCommand cmd = new DepositCommand();

    UserDto user =
        new UserDto(
            1,
            "John Doe",
            new AccountDto("john", UserRole.BIDDER),
            new WalletDto(BigDecimal.valueOf(1500), new HashMap<>()),
            "avatar_url");
    WalletUpdateResponse payload = new WalletUpdateResponse(user);
    PacketRes successPacket = PacketRes.of(ResponseType.DEPOSIT_RESULT, "Success", payload);
    cmd.execute(successPacket);
    assertTrue(updateNotified);

    // Failure case
    cmd.execute(PacketRes.error(ResponseType.DEPOSIT_RESULT, "Error"));
  }

  @Test
  public void testWalletUpdateCommand() {
    WalletUpdateCommand cmd = new WalletUpdateCommand();

    UserDto user =
        new UserDto(
            1,
            "John Doe",
            new AccountDto("john", UserRole.BIDDER),
            new WalletDto(BigDecimal.valueOf(1500), new HashMap<>()),
            "avatar_url");
    WalletUpdateResponse payload = new WalletUpdateResponse(user);
    PacketRes successPacket = PacketRes.of(ResponseType.WALLET_UPDATED, "Success", payload);
    cmd.execute(successPacket);
    assertTrue(updateNotified);
  }

  @Test
  public void testAuctionPaidNoticeCommand() {
    AuctionPaidNoticeCommand cmd = new AuctionPaidNoticeCommand();

    // 1. Failure case (isSuccess = false) -> hoan toan khong goi Platform.runLater
    cmd.execute(PacketRes.error(ResponseType.AUCTION_PAID_NOTICE, "Error"));

    // 2. Success case where data is null -> hoan toan khong goi Platform.runLater de tranh JaCoCo
    // classloading crash
    PacketRes successNullDataPacket =
        new PacketRes(true, ResponseType.AUCTION_PAID_NOTICE, "Success", null);
    cmd.execute(successNullDataPacket);
  }

  @Test
  public void testFetchAuctionDetailCommand() {
    FetchAuctionDetailCommand cmd = new FetchAuctionDetailCommand();

    UserDto sellerDto =
        new UserDto(
            1,
            "John Doe",
            new AccountDto("john", UserRole.SELLER),
            new WalletDto(BigDecimal.valueOf(1500), new HashMap<>()),
            "avatar_url");
    ItemDto item =
        new ItemDto(10, 1, "Vase", "Old Vase", 500L, 50L, ItemType.ART, false, "url", sellerDto);
    AuctionDto auction =
        new AuctionDto(
            123,
            10,
            1,
            null,
            AuctionStatus.OPEN,
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            500L,
            0,
            1,
            LocalDateTime.now(),
            LocalDateTime.now(),
            item,
            sellerDto,
            null,
            new ArrayList<>());
    AuctionDetailResponse payload = new AuctionDetailResponse(auction);
    PacketRes packet = PacketRes.of(ResponseType.AUCTION_RESULT, "Success", payload);
    cmd.execute(packet);

    assertTrue(updateNotified);
  }

  @Test
  public void testFetchAuctionHistoryCommand() {
    FetchAuctionHistoryCommand cmd = new FetchAuctionHistoryCommand();

    List<AuctionPreview> auctions = new ArrayList<>();
    auctions.add(
        new AuctionPreview(
            123,
            10,
            "Vase",
            "url",
            ItemType.ART,
            AuctionStatus.OPEN,
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            500L,
            500L,
            50L,
            1,
            new UserPreview(1, "John Doe", "john", UserRole.SELLER, "avatar_url")));
    AuctionHistoryResponse payload = new AuctionHistoryResponse(auctions, true);
    PacketRes packet = PacketRes.of(ResponseType.AUCTION_HISTORY_RESULT, "Success", payload);
    cmd.execute(packet);

    assertTrue(updateNotified);
  }

  @Test
  public void testFetchAuctionResultCommand() {
    FetchAuctionResultCommand cmd = new FetchAuctionResultCommand();

    UserDto winner =
        new UserDto(
            2,
            "Bidder A",
            new AccountDto("bidder", UserRole.BIDDER),
            new WalletDto(BigDecimal.valueOf(1500), new HashMap<>()),
            "avatar_url");
    AuctionResultResponse payload = new AuctionResultResponse(123L, winner, 1500L);
    PacketRes packet = PacketRes.of(ResponseType.AUCTION_RESULT_FETCHED, "Success", payload);
    cmd.execute(packet);

    assertTrue(updateNotified);
  }

  @Test
  public void testFetchAuctionSummariesCommand() {
    FetchAuctionSummariesCommand cmd = new FetchAuctionSummariesCommand();

    List<AuctionPreview> auctions = new ArrayList<>();
    auctions.add(
        new AuctionPreview(
            123,
            10,
            "Vase",
            "url",
            ItemType.ART,
            AuctionStatus.OPEN,
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            500L,
            500L,
            50L,
            1,
            new UserPreview(1, "John Doe", "john", UserRole.SELLER, "avatar_url")));
    AuctionSummariesResponse payload = new AuctionSummariesResponse(auctions);
    PacketRes packet = PacketRes.of(ResponseType.AUCTION_SUMMARIES_RESULT, "Success", payload);
    cmd.execute(packet);

    assertTrue(updateNotified);
  }

  @Test
  public void testFetchItemImageCommand() {
    FetchItemImageCommand cmd = new FetchItemImageCommand();

    FetchItemImageResponse payload = new FetchItemImageResponse(10, "base64_string_here");
    PacketRes packet = PacketRes.of(ResponseType.FETCH_ITEM_IMAGE, "Success", payload);
    cmd.execute(packet);

    assertTrue(updateNotified);
  }

  @Test
  public void testPlaceBidCommand() {
    PlaceBidCommand cmd = new PlaceBidCommand();

    PlaceBidResponse payload = new PlaceBidResponse(123, 1200L, 2);
    PacketRes packet = PacketRes.of(ResponseType.PLACE_BID_RESULT, "Success", payload);
    cmd.execute(packet);

    assertTrue(updateNotified);
  }

  @Test
  public void testRegisterCommand() {
    RegisterCommand cmd = new RegisterCommand();

    UserDto user =
        new UserDto(
            1,
            "John Doe",
            new AccountDto("john", UserRole.BIDDER),
            new WalletDto(BigDecimal.valueOf(1500), new HashMap<>()),
            "avatar_url");
    RegisterResponse payload = new RegisterResponse(user);
    PacketRes packet = PacketRes.of(ResponseType.REGISTER_RESULT, "Success", payload);
    cmd.execute(packet);

    // RegisterCommand thuc hien notifyMessage chu khong co notifyUpdate
    assertTrue(messageNotified);
  }

  @Test
  public void testUploadImageCommand() {
    UploadImageCommand cmd = new UploadImageCommand();

    UploadImageResponse payload = new UploadImageResponse(10, "path/to/server_data/image.png");
    PacketRes packet = PacketRes.of(ResponseType.UPLOAD_IMAGE, "Success", payload);
    cmd.execute(packet);

    assertTrue(updateNotified);
  }

  @Test
  public void testChatCommand() {
    ChatCommand cmd = new ChatCommand();

    ChatResponse payload = new ChatResponse(1, "john", "Hello, Room", LocalDateTime.now());
    PacketRes packet = PacketRes.of(ResponseType.CHAT_RESULT, "Success", payload);
    cmd.execute(packet);

    assertNotNull(chatNotified);
    assertEquals("john", chatNotified.sender());
  }

  @Test
  public void testErrorCommand() {
    ErrorCommand cmd = new ErrorCommand();

    PacketRes packet = PacketRes.error(ResponseType.ERROR, "Generic System Error");
    cmd.execute(packet);

    assertTrue(messageNotified);
  }
}
