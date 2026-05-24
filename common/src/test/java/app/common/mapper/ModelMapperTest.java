package app.common.mapper;

import static org.junit.jupiter.api.Assertions.*;

import app.common.dto.*;
import app.common.enums.*;
import app.common.models.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Lop kiem thu cho ModelMapper. Kiem tra viec anh xa qua lai giua DTO va Model. Viet bang tieng
 * Viet khong dau de giai thich.
 */
public class ModelMapperTest {

  /** Test anh xa Account. */
  @Test
  public void testAccountMapping() {
    // Null checks
    assertNull(ModelMapper.toAccountDto(null));
    assertNull(ModelMapper.toAccountModel(null));

    // Convert Model -> DTO
    Account account = new Account("username", "password", UserRole.BIDDER);
    AccountDto dto = ModelMapper.toAccountDto(account);
    assertNotNull(dto);
    assertEquals("username", dto.username());
    assertEquals(UserRole.BIDDER, dto.role());

    // Convert DTO -> Model
    Account model = ModelMapper.toAccountModel(dto);
    assertNotNull(model);
    assertEquals("username", model.getUsername());
    assertEquals(UserRole.BIDDER, model.getRole());
    assertNull(model.getPassword());
  }

  /** Test anh xa Wallet. */
  @Test
  public void testWalletMapping() {
    // Null checks
    assertNull(ModelMapper.toWalletDto(null));
    assertNull(ModelMapper.toWalletModel(null));

    // Convert Model -> DTO
    Map<String, BigDecimal> frozen = new HashMap<>();
    frozen.put("1", new BigDecimal("200"));
    Wallet wallet = new Wallet(new BigDecimal("1000"), frozen);
    WalletDto dto = ModelMapper.toWalletDto(wallet);
    assertNotNull(dto);
    assertEquals(0, dto.availableBalance().compareTo(new BigDecimal("1000")));
    assertEquals(0, dto.frozenFunds().get("1").compareTo(new BigDecimal("200")));

    // Convert DTO -> Model
    Wallet model = ModelMapper.toWalletModel(dto);
    assertNotNull(model);
    assertEquals(0, model.getAvailableBalance().compareTo(new BigDecimal("1000")));
    assertEquals(0, model.getFrozenFundsSnapshot().get("1").compareTo(new BigDecimal("200")));
  }

  /** Test anh xa User. */
  @Test
  public void testUserMapping() {
    // Null checks
    assertNull(ModelMapper.toUserDto(null));
    assertNull(ModelMapper.toUserModel(null));

    // Convert Model -> DTO
    User user =
        new User(
            1,
            "Nguyen Van A",
            new Account("nva", "pass", UserRole.BIDDER),
            new Wallet(new BigDecimal("5000")));
    UserDto dto = ModelMapper.toUserDto(user);
    assertNotNull(dto);
    assertEquals(1, dto.id());
    assertEquals("Nguyen Van A", dto.name());
    assertEquals("nva", dto.account().username());
    assertEquals(0, dto.wallet().availableBalance().compareTo(new BigDecimal("5000")));

    // Convert DTO -> Model
    User model = ModelMapper.toUserModel(dto);
    assertNotNull(model);
    assertEquals(1, model.getId());
    assertEquals("Nguyen Van A", model.getName());
    assertEquals("nva", model.getAccount().getUsername());
    assertEquals(0, model.getWallet().getAvailableBalance().compareTo(new BigDecimal("5000")));

    // Convert DTO without wallet -> Model
    UserDto dtoNoWallet =
        new UserDto(2, "Nguyen Van B", new AccountDto("nvb", UserRole.SELLER), null, null);
    User modelNoWallet = ModelMapper.toUserModel(dtoNoWallet);
    assertNotNull(modelNoWallet);
    assertEquals(2, modelNoWallet.getId());
    assertNull(modelNoWallet.getWallet());
  }

  /** Test anh xa Bid. */
  @Test
  public void testBidMapping() {
    // Null checks
    assertNull(ModelMapper.toBidDto(null));
    assertNull(ModelMapper.toBidModel(null));

    // Convert Model -> DTO
    LocalDateTime now = LocalDateTime.now();
    Bid bid = new Bid(10, 100, 1, "Nguyen Van A", 1500L, now, true);
    User bidder =
        new User(1, "Nguyen Van A", new Account("nva", "pass", UserRole.BIDDER), new Wallet());
    bid.setBidder(bidder);

    BidDto dto = ModelMapper.toBidDto(bid);
    assertNotNull(dto);
    assertEquals(10, dto.id());
    assertEquals(100, dto.auctionId());
    assertEquals(1, dto.bidderId());
    assertEquals("Nguyen Van A", dto.bidderName());
    assertEquals(1500L, dto.amount());
    assertEquals(now, dto.createAt());
    assertTrue(dto.isAutoBid());
    assertNotNull(dto.bidder());

    // Convert DTO -> Model
    Bid model = ModelMapper.toBidModel(dto);
    assertNotNull(model);
    assertEquals(10, model.getId());
    assertEquals(100, model.getAuctionId());
    assertEquals(1, model.getBidderId());
    assertEquals("Nguyen Van A", model.getBidderName());
    assertEquals(1500L, model.getAmount());
    assertEquals(now, model.getCreateAt());
    assertTrue(model.isAutoBid());
    assertNotNull(model.getBidder());
  }

  /** Test anh xa Item. */
  @Test
  public void testItemMapping() {
    // Null checks
    assertNull(ModelMapper.toItemDto(null));
    assertNull(ModelMapper.toItemModel(null));

    // Convert Model -> DTO
    Item item =
        ItemFactory.createItem(5, "iPhone 15", 2, "Test desc", 1000L, 100L, ItemType.ELECTRONICS);
    item.setDeleted(true);
    item.setImageUrl("http://image.com");
    User seller =
        new User(2, "Seller B", new Account("sellerb", "pass", UserRole.SELLER), new Wallet());
    item.setSeller(seller);

    ItemDto dto = ModelMapper.toItemDto(item);
    assertNotNull(dto);
    assertEquals(5, dto.id());
    assertEquals(2, dto.sellerId());
    assertEquals("iPhone 15", dto.name());
    assertEquals("Test desc", dto.description());
    assertEquals(1000L, dto.startingPrice());
    assertEquals(100L, dto.stepPrice());
    assertEquals(ItemType.ELECTRONICS, dto.type());
    assertTrue(dto.deleted());
    assertEquals("http://image.com", dto.imageUrl());
    assertNotNull(dto.seller());

    // Convert DTO -> Model
    Item model = ModelMapper.toItemModel(dto);
    assertNotNull(model);
    assertEquals(5, model.getId());
    assertEquals(2, model.getSellerId());
    assertEquals("iPhone 15", model.getName());
    assertEquals("Test desc", model.getDescription());
    assertEquals(1000L, model.getStartingPrice());
    assertEquals(100L, model.getStepPrice());
    assertEquals(ItemType.ELECTRONICS, model.getType());
    assertTrue(model.isDeleted());
    assertEquals("http://image.com", model.getImageUrl());
    assertNotNull(model.getSeller());
  }

  /** Test anh xa Auction. */
  @Test
  public void testAuctionMapping() {
    // Null checks
    assertNull(ModelMapper.toAuctionDto(null));
    assertNull(ModelMapper.toAuctionModel(null));

    // Create Auction Model
    LocalDateTime now = LocalDateTime.now();
    Auction auction =
        new Auction(
            1, 10, 2, 3, AuctionStatus.RUNNING, now, now.plusHours(2), 1200L, 1, 5, now, now);
    Item item =
        ItemFactory.createItem(10, "iPhone 15", 2, "Test desc", 1000L, 100L, ItemType.ELECTRONICS);
    User seller =
        new User(2, "Seller B", new Account("sellerb", "pass", UserRole.SELLER), new Wallet());
    User winner =
        new User(3, "Winner C", new Account("winnerc", "pass", UserRole.BIDDER), new Wallet());
    Bid bid = new Bid(100, 1, 3, "Winner C", 1200L, now.plusMinutes(5), false);

    auction.setItem(item);
    auction.setSeller(seller);
    auction.setWinner(winner);
    auction.setBids(Collections.singletonList(bid));

    // Convert Model -> DTO
    AuctionDto dto = ModelMapper.toAuctionDto(auction);
    assertNotNull(dto);
    assertEquals(1, dto.id());
    assertEquals(10, dto.itemId());
    assertEquals(2, dto.sellerId());
    assertEquals(3, dto.winnerId());
    assertEquals(AuctionStatus.RUNNING, dto.status());
    assertEquals(now, dto.startTime());
    assertEquals(1200L, dto.highestBid());
    assertEquals(1, dto.extendedCount());
    assertEquals(5, dto.version());
    assertNotNull(dto.item());
    assertNotNull(dto.seller());
    assertNotNull(dto.winner());
    assertNotNull(dto.bids());
    assertEquals(1, dto.bids().size());

    // Convert DTO -> Model
    Auction model = ModelMapper.toAuctionModel(dto);
    assertNotNull(model);
    assertEquals(1, model.getId());
    assertEquals(10, model.getItemId());
    assertEquals(2, model.getSellerId());
    assertEquals(3, model.getWinnerId());
    assertEquals(AuctionStatus.RUNNING, model.getStatus());
    assertEquals(now, model.getStartTime());
    assertEquals(1200L, model.getHighestBid());
    assertEquals(1, model.getExtendedCount());
    assertEquals(5, model.getVersion());
    assertNotNull(model.getItem());
    assertNotNull(model.getSeller());
    assertNotNull(model.getWinner());
    assertNotNull(model.getBids());
    assertEquals(1, model.getBids().size());
  }

  /** Test anh xa preview. */
  @Test
  public void testPreviewMapping() {
    assertNull(ModelMapper.toUserPreview(null));
    assertNull(ModelMapper.toItemPreview(null));
    assertNull(ModelMapper.toAuctionPreview(null));

    LocalDateTime now = LocalDateTime.now();
    User user =
        new User(
            1, "User Name", new Account("username", "password", UserRole.BIDDER), new Wallet());
    Item item = ItemFactory.createItem("Item Name", 1, "Desc", 100L, 10L, ItemType.ART);
    item.setId(1);

    UserPreview userPreview = ModelMapper.toUserPreview(user);
    assertEquals(1, userPreview.userId());
    assertEquals("User Name", userPreview.name());
    assertEquals("username", userPreview.username());
    assertEquals(UserRole.BIDDER, userPreview.role());

    ItemPreview itemPreview = ModelMapper.toItemPreview(item);
    assertEquals("Item Name", itemPreview.name());
    assertEquals(ItemType.ART, itemPreview.itemType());

    Auction auction = new Auction(1, 1, now.plusDays(1), 100L);
    auction.setId(10);
    auction.setItem(item);
    auction.setSeller(user);
    auction.setVersion(2);

    AuctionPreview auctionPreview = ModelMapper.toAuctionPreview(auction);
    assertEquals(10, auctionPreview.auctionId());
    assertEquals(1, auctionPreview.itemId());
    assertEquals("Item Name", auctionPreview.itemName());
    assertEquals(ItemType.ART, auctionPreview.itemType());
    assertEquals(AuctionStatus.OPEN, auctionPreview.status());
    assertEquals(100L, auctionPreview.startingPrice());
    assertEquals(10L, auctionPreview.stepPrice());
    assertEquals(2, auctionPreview.version());
    assertEquals("User Name", auctionPreview.seller().name());
  }
}
