package app.server.service;

import static org.junit.jupiter.api.Assertions.*;

import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.models.Account;
import app.common.models.Auction;
import app.common.models.User;
import app.common.models.Wallet;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

public class OwnershipGuardTest {

  @Test
  public void testRequireValidActorSuccess() {
    User actor =
        new User(10, "Tester", new Account("username", "password", UserRole.BIDDER), new Wallet());
    assertDoesNotThrow(() -> OwnershipGuard.requireValidActor(actor));
  }

  @Test
  public void testRequireValidActorNull() {
    assertThrows(ServiceException.class, () -> OwnershipGuard.requireValidActor(null));
  }

  @Test
  public void testRequireValidActorInvalidId() {
    User actor =
        new User(0, "Tester", new Account("username", "password", UserRole.BIDDER), new Wallet());
    assertThrows(ServiceException.class, () -> OwnershipGuard.requireValidActor(actor));
  }

  @Test
  public void testRequireAuctionOwnerOrAdminAdminSuccess() {
    User admin = new User(1, "Admin", new Account("admin", "admin", UserRole.ADMIN), new Wallet());
    Auction auction = new Auction(1, 2, LocalDateTime.now().plusDays(1), 100L);

    assertDoesNotThrow(() -> OwnershipGuard.requireAuctionOwnerOrAdmin(auction, admin));
  }

  @Test
  public void testRequireAuctionOwnerOrAdminOwnerSuccess() {
    User owner =
        new User(2, "Seller", new Account("seller", "pass", UserRole.SELLER), new Wallet());
    Auction auction =
        new Auction(1, 2, LocalDateTime.now().plusDays(1), 100L); // itemId=1, sellerId=2

    assertDoesNotThrow(() -> OwnershipGuard.requireAuctionOwnerOrAdmin(auction, owner));
  }

  @Test
  public void testRequireAuctionOwnerOrAdminForbidden() {
    User bidder =
        new User(3, "Bidder", new Account("bidder", "pass", UserRole.BIDDER), new Wallet());
    Auction auction = new Auction(1, 2, LocalDateTime.now().plusDays(1), 100L);

    assertThrows(
        ServiceException.class, () -> OwnershipGuard.requireAuctionOwnerOrAdmin(auction, bidder));
  }

  @Test
  public void testRequireSellerOwnerOrAdminAdminSuccess() {
    assertDoesNotThrow(() -> OwnershipGuard.requireSellerOwnerOrAdmin(1, UserRole.ADMIN, 2));
  }

  @Test
  public void testRequireSellerOwnerOrAdminOwnerSuccess() {
    assertDoesNotThrow(() -> OwnershipGuard.requireSellerOwnerOrAdmin(2, UserRole.SELLER, 2));
  }

  @Test
  public void testRequireSellerOwnerOrAdminForbidden() {
    assertThrows(
        ServiceException.class,
        () -> OwnershipGuard.requireSellerOwnerOrAdmin(3, UserRole.BIDDER, 2));
  }

  @Test
  public void testRequireSellerOwnerOrAdminInvalidData() {
    assertThrows(
        ServiceException.class,
        () -> OwnershipGuard.requireSellerOwnerOrAdmin(0, UserRole.SELLER, 2));
    assertThrows(
        ServiceException.class, () -> OwnershipGuard.requireSellerOwnerOrAdmin(2, null, 2));
    assertThrows(
        ServiceException.class,
        () -> OwnershipGuard.requireSellerOwnerOrAdmin(2, UserRole.SELLER, 0));
  }
}
