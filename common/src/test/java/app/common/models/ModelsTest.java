package app.common.models;

import static org.junit.jupiter.api.Assertions.*;

import app.common.enums.ItemType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Lop kiem thu cho cac Model va Wallet. Viet bang tieng Viet khong dau de giai thich. */
public class ModelsTest {

  /**
   * Test toan bo cac phuong thuc trong Wallet (deposit, withdraw, freeze, release, commit,
   * serialize).
   */
  @Test
  public void testWalletLogic() {
    Wallet wallet = new Wallet(new BigDecimal("1000.00"));
    assertEquals(0, wallet.getAvailableBalance().compareTo(new BigDecimal("1000")));
    assertEquals(0, wallet.getTotalBalance().compareTo(new BigDecimal("1000")));

    // Deposit hop le va khong hop le
    wallet.deposit(new BigDecimal("500"));
    assertEquals(0, wallet.getAvailableBalance().compareTo(new BigDecimal("1500")));
    assertThrows(IllegalArgumentException.class, () -> wallet.deposit(BigDecimal.ZERO));
    assertThrows(IllegalArgumentException.class, () -> wallet.deposit(new BigDecimal("-100")));

    // Withdraw hop le va khong hop le
    wallet.withdraw(new BigDecimal("300"));
    assertEquals(0, wallet.getAvailableBalance().compareTo(new BigDecimal("1200")));
    assertThrows(IllegalArgumentException.class, () -> wallet.withdraw(new BigDecimal("2000")));
    assertThrows(IllegalArgumentException.class, () -> wallet.withdraw(new BigDecimal("-50")));

    // Freeze quy cho phien dau gia
    BigDecimal prev = wallet.setFrozenAmount("auction_1", new BigDecimal("400"));
    assertEquals(BigDecimal.ZERO, prev);
    assertEquals(0, wallet.getFrozenAmount("auction_1").compareTo(new BigDecimal("400")));
    assertEquals(0, wallet.getAvailableBalance().compareTo(new BigDecimal("800")));
    assertEquals(0, wallet.getTotalBalance().compareTo(new BigDecimal("1200")));

    // Tang so tien freeze len 600
    prev = wallet.setFrozenAmount("auction_1", new BigDecimal("600"));
    assertEquals(0, prev.compareTo(new BigDecimal("400")));
    assertEquals(0, wallet.getFrozenAmount("auction_1").compareTo(new BigDecimal("600")));
    assertEquals(0, wallet.getAvailableBalance().compareTo(new BigDecimal("600")));

    // Giam so tien freeze xuong 200
    prev = wallet.setFrozenAmount("auction_1", new BigDecimal("200"));
    assertEquals(0, prev.compareTo(new BigDecimal("600")));
    assertEquals(0, wallet.getFrozenAmount("auction_1").compareTo(new BigDecimal("200")));
    assertEquals(0, wallet.getAvailableBalance().compareTo(new BigDecimal("1000")));

    // Dat gia freeze vuot qua so du khả dung
    assertThrows(
        IllegalArgumentException.class,
        () -> wallet.setFrozenAmount("auction_1", new BigDecimal("1500")));
    // Kiem tra freeze so am
    assertThrows(
        IllegalArgumentException.class,
        () -> wallet.setFrozenAmount("auction_1", new BigDecimal("-10")));

    // Commit freeze (khi thang dau gia, tien bi tru luon)
    BigDecimal committed = wallet.commitFrozen("auction_1");
    assertEquals(0, committed.compareTo(new BigDecimal("200")));
    assertEquals(BigDecimal.ZERO, wallet.getFrozenAmount("auction_1"));
    assertEquals(0, wallet.getAvailableBalance().compareTo(new BigDecimal("1000")));
    assertEquals(0, wallet.getTotalBalance().compareTo(new BigDecimal("1000")));

    // Thu freeze va sau do release (giai phong quy khi thua dau gia)
    wallet.setFrozenAmount("auction_2", new BigDecimal("300"));
    assertEquals(0, wallet.getAvailableBalance().compareTo(new BigDecimal("700")));
    BigDecimal released = wallet.releaseFrozen("auction_2");
    assertEquals(0, released.compareTo(new BigDecimal("300")));
    assertEquals(0, wallet.getAvailableBalance().compareTo(new BigDecimal("1000")));

    // Release phien khong ton tai
    assertEquals(BigDecimal.ZERO, wallet.releaseFrozen("auction_unknown"));

    // Commit phien khong ton tai
    assertEquals(BigDecimal.ZERO, wallet.commitFrozen("auction_unknown"));

    // Serialize va Parse
    wallet.setFrozenAmount("auction_3", new BigDecimal("150"));
    String jsonStr = wallet.serializeFrozenFunds();
    assertNotNull(jsonStr);

    Map<String, BigDecimal> parsed = Wallet.parseFrozenFunds(jsonStr);
    assertNotNull(parsed);
    assertEquals(0, parsed.get("auction_3").compareTo(new BigDecimal("150")));

    // Test parse string rong
    assertTrue(Wallet.parseFrozenFunds("").isEmpty());
    assertTrue(Wallet.parseFrozenFunds(null).isEmpty());
  }

  /** Test ItemFactory va cac lop con cua Item nhu Art, Electronics, Vehicle. */
  @Test
  public void testItemInheritance() {
    Item art =
        ItemFactory.createItem("Buc tranh Pho Co", 1, "Art description", 5000L, 500L, ItemType.ART);
    assertTrue(art instanceof Art);
    assertEquals(ItemType.ART, art.getType());

    Item elec =
        ItemFactory.createItem(
            "Macbook M3", 1, "Elec description", 15000L, 1000L, ItemType.ELECTRONICS);
    assertTrue(elec instanceof Electronics);
    assertEquals(ItemType.ELECTRONICS, elec.getType());

    Item veh =
        ItemFactory.createItem("Honda SH", 1, "Veh description", 80000L, 2000L, ItemType.VEHICLE);
    assertTrue(veh instanceof Vehicle);
    assertEquals(ItemType.VEHICLE, veh.getType());

    // Check entity getters and setters
    art.setId(10);
    assertEquals(10, art.getId());
  }

  /** Test AutoBid model. */
  @Test
  public void testAutoBidModel() {
    LocalDateTime now = LocalDateTime.now();
    AutoBid autoBid = new AutoBid(1, 100, 2, 5000L, 100L, true, now, now);
    assertEquals(1, autoBid.getId());
    assertEquals(100, autoBid.getAuctionId());
    assertEquals(2, autoBid.getUserId());
    assertEquals(5000L, autoBid.getMaxAmount());
    assertEquals(100L, autoBid.getIncrementAmount());
    assertTrue(autoBid.isEnabled());

    autoBid.setId(5);
    autoBid.setAuctionId(200);
    autoBid.setUserId(3);
    autoBid.setMaxAmount(6000L);
    autoBid.setIncrementAmount(200L);
    autoBid.setEnabled(false);

    assertEquals(5, autoBid.getId());
    assertEquals(200, autoBid.getAuctionId());
    assertEquals(3, autoBid.getUserId());
    assertEquals(6000L, autoBid.getMaxAmount());
    assertEquals(200L, autoBid.getIncrementAmount());
    assertFalse(autoBid.isEnabled());
  }
}
