package app.client.manager;

import static org.junit.jupiter.api.Assertions.*;

import app.client.Client;
import app.common.dto.*;
import app.common.enums.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * ClientRequestServiceTest. Kiem thu toan bo cac phuong thuc yeu cau gui goi tin len server. Su
 * dung StringWriter thuc te thay vi Mockito Mock de tuong thich 100% JDK 25.
 */
public class ClientRequestServiceTest {
  private Client client;
  private StringWriter sw;
  private BufferedWriter realWriter;

  @BeforeEach
  void setUp() throws Exception {
    client = Client.getInstance();
    sw = new StringWriter();
    realWriter = new BufferedWriter(sw);

    // Reset thong so ket noi local cua Client thong qua Reflection de gia lap da ket noi
    setPrivateField(client, "connected", true);
    setPrivateField(client, "closed", false);
    setPrivateField(client, "writer", realWriter);
  }

  private void setPrivateField(Object obj, String fieldName, Object value) throws Exception {
    Field field = Client.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(obj, value);
  }

  @Test
  public void testAllRequests() throws Exception {
    ClientRequestService service = ClientRequestService.getInstance();

    assertTrue(service.isConnected());

    // 1. Login
    assertDoesNotThrow(() -> service.login(new LoginRequest("user", "pass")));

    // 2. Register
    assertDoesNotThrow(
        () -> service.register(new RegisterRequest("John", "john", "pass", UserRole.BIDDER)));

    // 3. Create Auction
    assertDoesNotThrow(
        () ->
            service.createAuction(
                new CreateAuctionRequest(
                    "Vase",
                    "Old Vase",
                    1000L,
                    100L,
                    ItemType.ART,
                    60,
                    LocalDateTime.now().plusHours(1))));

    // 4. Update Auction
    assertDoesNotThrow(
        () ->
            service.updateAuction(
                new UpdateAuctionRequest(
                    1,
                    "Vase",
                    "Old Vase",
                    1000L,
                    100L,
                    ItemType.ART,
                    60,
                    LocalDateTime.now().plusHours(1),
                    0)));

    // 5. Fetch summaries
    assertDoesNotThrow(() -> service.fetchAuctionSummaries());

    // 6. Fetch history
    assertDoesNotThrow(() -> service.fetchAuctionHistory(2));

    // 7. Fetch detail
    assertDoesNotThrow(() -> service.fetchAuctionDetail(10, 1));

    // 8. Unwatch
    assertDoesNotThrow(() -> service.unwatchAuction());

    // 10. Place bid
    assertDoesNotThrow(() -> service.placeBid(10, 1500L));

    // 11. Deposit
    assertDoesNotThrow(() -> service.deposit(BigDecimal.valueOf(500)));

    // 12. Settle wallet
    assertDoesNotThrow(() -> service.settleWallet(10));

    // 13. Chat
    assertDoesNotThrow(() -> service.chat(new ChatRequest("Hello")));

    // 14. Fetch image
    assertDoesNotThrow(() -> service.fetchItemImage(5, "path/to/img"));

    // 15. Upload image
    File tempFile = File.createTempFile("temp_img", ".jpg");
    tempFile.deleteOnExit();
    assertDoesNotThrow(() -> service.uploadImage(5, tempFile));

    // Inspect the written JSONs
    realWriter.flush();
    String writtenData = sw.toString();
    assertNotNull(writtenData);
    assertTrue(writtenData.contains("LOGIN"));
    assertTrue(writtenData.contains("REGISTER"));
    assertTrue(writtenData.contains("CREATE_AUCTION"));
  }
}
