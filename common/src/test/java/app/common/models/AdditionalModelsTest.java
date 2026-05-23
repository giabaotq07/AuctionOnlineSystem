package app.common.models;

import static org.junit.jupiter.api.Assertions.*;

import app.common.enums.AuctionStatus;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Bo sung test cho cac model de dat do phu tren 85%. Kiem thu day du cac truong hop bat thuong, nem
 * ngoai le, getter/setter. Viet bang tieng Viet khong dau.
 */
public class AdditionalModelsTest {

  // Dung de test lop truu tuong Entity
  static class DummyEntity extends Entity {
    public DummyEntity() {
      super();
    }

    public DummyEntity(int id) {
      super(id);
    }
  }

  @Test
  public void testAuctionConstructorValidation() {
    LocalDateTime future = LocalDateTime.now().plusDays(1);

    // Test tham so khong hop le cho constructor rut gon
    assertThrows(IllegalArgumentException.class, () -> new Auction(0, 1, future, 100L));
    assertThrows(IllegalArgumentException.class, () -> new Auction(1, -1, future, 100L));
    assertThrows(NullPointerException.class, () -> new Auction(1, 1, null, 100L));
    assertThrows(IllegalArgumentException.class, () -> new Auction(1, 1, future, -10L));

    // Test constructor day du voi cac tham so am
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Auction(
                -1,
                1,
                1,
                null,
                AuctionStatus.OPEN,
                LocalDateTime.now(),
                future,
                100L,
                0,
                0,
                LocalDateTime.now(),
                LocalDateTime.now()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Auction(
                1,
                -1,
                1,
                null,
                AuctionStatus.OPEN,
                LocalDateTime.now(),
                future,
                100L,
                0,
                0,
                LocalDateTime.now(),
                LocalDateTime.now()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Auction(
                1,
                1,
                -1,
                null,
                AuctionStatus.OPEN,
                LocalDateTime.now(),
                future,
                100L,
                0,
                0,
                LocalDateTime.now(),
                LocalDateTime.now()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Auction(
                1,
                1,
                1,
                -5,
                AuctionStatus.OPEN,
                LocalDateTime.now(),
                future,
                100L,
                0,
                0,
                LocalDateTime.now(),
                LocalDateTime.now()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Auction(
                1,
                1,
                1,
                null,
                AuctionStatus.OPEN,
                LocalDateTime.now(),
                future,
                -5L,
                0,
                0,
                LocalDateTime.now(),
                LocalDateTime.now()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Auction(
                1,
                1,
                1,
                null,
                AuctionStatus.OPEN,
                LocalDateTime.now(),
                future,
                100L,
                -1,
                0,
                LocalDateTime.now(),
                LocalDateTime.now()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Auction(
                1,
                1,
                1,
                null,
                AuctionStatus.OPEN,
                LocalDateTime.now(),
                future,
                100L,
                0,
                -1,
                LocalDateTime.now(),
                LocalDateTime.now()));
  }

  @Test
  public void testAuctionStateTransitions() {
    LocalDateTime future = LocalDateTime.now().plusDays(1);
    Auction auction = new Auction(1, 1, future, 100L);

    // Kiem tra trang thai ban dau
    assertEquals(AuctionStatus.OPEN, auction.getStatus());
    assertFalse(auction.isRunning());

    // Start thanh cong
    auction.start();
    assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    assertTrue(auction.isRunning());
    assertNotNull(auction.getStartTime());

    // Thu start lai se bi loi
    assertThrows(IllegalStateException.class, () -> auction.start());

    // Finish thanh cong tu RUNNING
    auction.finish();
    assertEquals(AuctionStatus.FINISHED, auction.getStatus());

    // Thu finish lai se bi loi
    assertThrows(IllegalStateException.class, () -> auction.finish());

    // Test mark paid
    auction.markPaid();
    assertEquals(AuctionStatus.PAID, auction.getStatus());

    // Thu mark paid lai se bi loi
    assertThrows(IllegalStateException.class, () -> auction.markPaid());

    // Reset ve OPEN de test finish truc tiep tu OPEN
    Auction auction2 = new Auction(1, 1, future, 100L);
    auction2.finish(5);
    assertEquals(AuctionStatus.FINISHED, auction2.getStatus());
    assertEquals(5, auction2.getWinnerId());

    // Test invalid winnerId khi finish
    Auction auction3 = new Auction(1, 1, future, 100L);
    assertThrows(IllegalArgumentException.class, () -> auction3.finish(-1));
  }

  @Test
  public void testAuctionCancel() {
    LocalDateTime future = LocalDateTime.now().plusDays(1);

    // Huy khi OPEN -> hop le
    Auction auction1 = new Auction(1, 1, future, 100L);
    auction1.cancel();
    assertEquals(AuctionStatus.CANCELED, auction1.getStatus());

    // Huy khi RUNNING -> hop le
    Auction auction2 = new Auction(1, 1, future, 100L);
    auction2.start();
    auction2.cancel();
    assertEquals(AuctionStatus.CANCELED, auction2.getStatus());

    // Huy khi FINISHED -> loi
    Auction auction3 = new Auction(1, 1, future, 100L);
    auction3.finish();
    assertThrows(IllegalStateException.class, () -> auction3.cancel());

    // Huy khi PAID -> loi
    Auction auction4 = new Auction(1, 1, future, 100L);
    auction4.finish();
    auction4.markPaid();
    assertThrows(IllegalStateException.class, () -> auction4.cancel());
  }

  @Test
  public void testAuctionBiddingAndExtension() {
    LocalDateTime future = LocalDateTime.now().plusSeconds(10);
    Auction auction = new Auction(1, 1, future, 100L);

    // updateHighestBid hop le va khong hop le
    auction.updateHighestBid(150L, 2);
    assertEquals(150L, auction.getHighestBid());
    assertEquals(2, auction.getWinnerId());

    assertThrows(IllegalArgumentException.class, () -> auction.updateHighestBid(140L, 3));
    assertThrows(IllegalArgumentException.class, () -> auction.updateHighestBid(150L, 3));
    assertThrows(IllegalArgumentException.class, () -> auction.updateHighestBid(200L, -1));

    // Gia han (extend)
    // extend loi khi chua RUNNING
    assertThrows(IllegalStateException.class, () -> auction.extend(30));

    auction.start();
    // extend loi neu thoi gian gia han <= 0
    assertThrows(IllegalArgumentException.class, () -> auction.extend(0));
    assertThrows(IllegalArgumentException.class, () -> auction.extend(-10));

    // extend thanh cong
    LocalDateTime oldEndTime = auction.getEndTime();
    auction.extend(60);
    assertEquals(oldEndTime.plusSeconds(60), auction.getEndTime());
    assertEquals(1, auction.getExtendedCount());
  }

  @Test
  public void testAuctionExpiration() {
    LocalDateTime past = LocalDateTime.now().minusSeconds(10);
    Auction auction1 = new Auction(1, 1, past, 100L);

    // Kiem tra het han voi Clock he thong
    assertTrue(auction1.isExpired());

    LocalDateTime future = LocalDateTime.now().plusDays(1);
    Auction auction2 = new Auction(1, 1, future, 100L);
    assertFalse(auction2.isExpired());

    // Kiem tra voi Fixed Clock
    Instant fixedInstant = Instant.parse("2026-05-23T12:00:00Z");
    Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
    LocalDateTime localDateTimeFixed = LocalDateTime.ofInstant(fixedInstant, ZoneId.of("UTC"));

    Auction auction3 = new Auction(1, 1, localDateTimeFixed.minusMinutes(1), 100L);
    assertTrue(auction3.isExpired(fixedClock));

    Auction auction4 = new Auction(1, 1, localDateTimeFixed.plusMinutes(1), 100L);
    assertFalse(auction4.isExpired(fixedClock));
  }

  @Test
  public void testAuctionSettersAndValidation() {
    LocalDateTime future = LocalDateTime.now().plusDays(1);
    Auction auction = new Auction(1, 1, future, 100L);

    // set/get Id
    auction.setId(50);
    assertEquals(50, auction.getId());
    assertThrows(IllegalArgumentException.class, () -> auction.setId(-1));

    // winnerId setter
    auction.setWinnerId(5);
    assertEquals(5, auction.getWinnerId());
    auction.setWinnerId(null);
    assertNull(auction.getWinnerId());
    assertThrows(IllegalArgumentException.class, () -> auction.setWinnerId(0));

    // status setter
    auction.setStatus(AuctionStatus.RUNNING);
    assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    assertThrows(NullPointerException.class, () -> auction.setStatus(null));

    // startTime setter
    LocalDateTime now = LocalDateTime.now();
    auction.setStartTime(now);
    assertEquals(now, auction.getStartTime());

    // endTime setter
    auction.setEndTime(future);
    assertEquals(future, auction.getEndTime());
    assertThrows(NullPointerException.class, () -> auction.setEndTime(null));

    // highestBid setter
    auction.setHighestBid(500L);
    assertEquals(500L, auction.getHighestBid());
    assertThrows(IllegalArgumentException.class, () -> auction.setHighestBid(-1L));

    // extendedCount setter
    auction.setExtendedCount(3);
    assertEquals(3, auction.getExtendedCount());
    assertThrows(IllegalArgumentException.class, () -> auction.setExtendedCount(-1));

    // version setter & increment
    auction.setVersion(10);
    assertEquals(10, auction.getVersion());
    assertThrows(IllegalArgumentException.class, () -> auction.setVersion(-1));
    auction.incrementVersion();
    assertEquals(11, auction.getVersion());

    // updatedAt setter
    auction.setUpdatedAt(now);
    assertEquals(now, auction.getUpdatedAt());

    // bids helper
    assertNotNull(auction.getBids());
    auction.setBids(null);
    assertTrue(auction.getBids().isEmpty());

    Bid bid = new Bid(1, 1, 1, "bidderName", 150L, now, false);
    auction.addBid(bid);
    assertEquals(1, auction.getBids().size());
    auction.addBid(null);
    assertEquals(1, auction.getBids().size());

    List<Bid> bidList = new ArrayList<>();
    bidList.add(bid);
    auction.setBids(bidList);
    assertEquals(1, auction.getBids().size());
  }

  @Test
  public void testAuctionItemAndSellerGetters() {
    LocalDateTime future = LocalDateTime.now().plusDays(1);
    Auction auction = new Auction(1, 2, future, 100L);

    // Khi chua gan Item/User thuc te
    assertEquals(1, auction.getItemId());
    assertEquals(2, auction.getSellerId());
    assertNull(auction.getItemName());
    assertNull(auction.getImageUrl());
    assertNull(auction.getWinnerId());

    // Gan Item, Seller, Winner
    Item item = ItemFactory.createItem("Item Name", 10, "Desc", 100L, 10L, ItemType.ART);
    item.setId(10); // Dat Id cho item de get item id chinh xac
    Account sellerAccount = new Account("seller", "pass", UserRole.BIDDER);
    User seller = new User(20, "Seller Name", sellerAccount, new Wallet(BigDecimal.TEN));
    Account winnerAccount = new Account("winner", "pass", UserRole.BIDDER);
    User winner = new User(30, "Winner Name", winnerAccount, new Wallet(BigDecimal.TEN));

    auction.setItem(item);
    auction.setSeller(seller);
    auction.setWinner(winner);

    assertEquals(10, auction.getItemId());
    assertEquals(20, auction.getSellerId());
    assertEquals(30, auction.getWinnerId());
    assertEquals("Item Name", auction.getItemName());
    assertNull(auction.getImageUrl()); // URL null

    item.setImageUrl("http://image.png");
    assertEquals("http://image.png", auction.getImageUrl());

    assertNotNull(auction.toString());
  }

  @Test
  public void testUserValidation() {
    Account account = new Account("username", "password", UserRole.BIDDER);
    Wallet wallet = new Wallet(BigDecimal.TEN);

    // Kiem tra nem ngoai le constructor full
    assertThrows(IllegalArgumentException.class, () -> new User(1, null, account, wallet));
    assertThrows(IllegalArgumentException.class, () -> new User(1, "", account, wallet));
    assertThrows(IllegalArgumentException.class, () -> new User(1, "   ", account, wallet));
    assertThrows(NullPointerException.class, () -> new User(1, "Name", null, wallet));
    assertThrows(NullPointerException.class, () -> new User(1, "Name", account, null));

    // Kiem tra nem ngoai le constructor rut gon
    assertThrows(IllegalArgumentException.class, () -> new User(null, account, wallet));
    assertThrows(IllegalArgumentException.class, () -> new User("", account, wallet));
    assertThrows(NullPointerException.class, () -> new User("Name", null, wallet));
    assertThrows(NullPointerException.class, () -> new User("Name", account, null));

    // Test setName, getName, getAccount, getWallet, getRole
    User user = new User("Old Name", account, wallet);
    assertEquals("Old Name", user.getName());
    user.setName("New Name");
    assertEquals("New Name", user.getName());
    assertEquals(account, user.getAccount());
    assertEquals(wallet, user.getWallet());
    assertEquals(UserRole.BIDDER, user.getRole());

    // Test createPublicUser va publicView
    User publicUser = User.createPublicUser(5, "Public", account);
    assertEquals(5, publicUser.getId());
    assertNull(publicUser.getWallet());

    User pView = user.publicView();
    assertNotNull(pView);
    assertNull(pView.getWallet());
    assertNull(pView.getAccount().getPassword());
  }

  @Test
  public void testEntityClass() {
    Entity e1 = new DummyEntity(10);
    assertEquals(10, e1.getId());
    e1.setId(20);
    assertEquals(20, e1.getId());

    Entity e2 = new DummyEntity(20);
    assertEquals(e1, e2);
    assertEquals(e1.hashCode(), e2.hashCode());

    Entity e3 = new DummyEntity(30);
    assertNotEquals(e1, e3);
    assertNotEquals(e1, null);
    assertNotEquals(e1, new Object());

    Entity e4 = new DummyEntity();
    assertEquals(0, e4.getId());
  }

  @Test
  public void testAccountClass() {
    Account a = new Account("username", "password", UserRole.BIDDER);
    assertEquals("username", a.getUsername());
    assertEquals("password", a.getPassword());
    assertEquals(UserRole.BIDDER, a.getRole());

    a.setUsername("newuser");
    a.setPassword("newpass");
    a.setRole(UserRole.ADMIN);

    assertEquals("newuser", a.getUsername());
    assertEquals("newpass", a.getPassword());
    assertEquals(UserRole.ADMIN, a.getRole());
  }

  @Test
  public void testBidClass() {
    LocalDateTime now = LocalDateTime.now();
    Bid bid = new Bid(1, 10, 20, "bidderName", 500L, now, false);

    assertEquals(1, bid.getId());
    assertEquals(10, bid.getAuctionId());
    assertEquals(20, bid.getBidderId());
    assertEquals(500L, bid.getAmount());
    assertEquals(now, bid.getCreateAt());

    bid.setId(2);
    bid.setAuctionId(11);
    bid.setBidder(null); // Clear bidder object de setBidderId hoat dong chinh xac voi getBidderId
    bid.setBidderId(21);
    bid.setAmount(600L);
    bid.setAutoBid(true);

    assertEquals(2, bid.getId());
    assertEquals(11, bid.getAuctionId());
    assertEquals(21, bid.getBidderId());
    assertEquals(600L, bid.getAmount());
    assertTrue(bid.isAutoBid());

    assertNotNull(bid.toString());
  }

  @Test
  public void testItemClassAndSubclasses() {
    LocalDateTime now = LocalDateTime.now();
    Item item = ItemFactory.createItem("ItemName", 5, "ItemDesc", 1000L, 50L, ItemType.ELECTRONICS);

    assertEquals("ItemName", item.getName());
    assertEquals(5, item.getSellerId());
    assertEquals("ItemDesc", item.getDescription());
    assertEquals(1000L, item.getStartingPrice());
    assertEquals(50L, item.getStepPrice());
    assertEquals(ItemType.ELECTRONICS, item.getType());

    item.setName("NewItemName");
    item.setSellerId(6);
    item.setDescription("NewItemDesc");
    item.setStartingPrice(2000L);
    item.setStepPrice(100L);
    item.setImageUrl("image_url");
    item.setType(ItemType.VEHICLE);
    item.setCreatedAt(now);
    item.setUpdatedAt(now);

    assertEquals("NewItemName", item.getName());
    assertEquals(6, item.getSellerId());
    assertEquals("NewItemDesc", item.getDescription());
    assertEquals(2000L, item.getStartingPrice());
    assertEquals(100L, item.getStepPrice());
    assertEquals("image_url", item.getImageUrl());
    assertEquals(ItemType.VEHICLE, item.getType());
    assertEquals(now, item.getCreatedAt());
    assertEquals(now, item.getUpdatedAt());

    // Validators goi Constructor qua ItemFactory de kiem tra
    assertThrows(
        IllegalArgumentException.class,
        () -> ItemFactory.createItem("ItemName", 5, "ItemDesc", -1L, 50L, ItemType.ELECTRONICS));
    assertThrows(
        IllegalArgumentException.class,
        () -> ItemFactory.createItem("ItemName", 5, "ItemDesc", 1000L, 0L, ItemType.ELECTRONICS));
    assertThrows(
        IllegalArgumentException.class,
        () -> ItemFactory.createItem("ItemName", 0, "ItemDesc", 1000L, 50L, ItemType.ELECTRONICS));
    assertThrows(
        IllegalArgumentException.class,
        () -> ItemFactory.createItem(null, 5, "ItemDesc", 1000L, 50L, ItemType.ELECTRONICS));
    assertThrows(
        NullPointerException.class,
        () -> ItemFactory.createItem("ItemName", 5, "ItemDesc", 1000L, 50L, null));
  }
}
