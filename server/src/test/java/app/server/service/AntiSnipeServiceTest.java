package app.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.common.enums.AuctionStatus;
import app.common.models.Auction;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** Kiem thu AntiSnipeService voi cac truong hop bien. */
class AntiSnipeServiceTest {
  private static final ZoneId ZONE = ZoneId.of("UTC");
  private static final Instant NOW = Instant.parse("2026-05-21T00:00:00Z");

  @Test
  void apply_shouldExtendAuctionNearEndUsingInjectedClock() {
    LocalDateTime endTime = LocalDateTime.ofInstant(NOW.plusSeconds(20), ZONE);
    Auction auction = new Auction(1, 2, endTime, 1000);
    auction.start();

    new AntiSnipeService(Clock.fixed(NOW, ZONE)).apply(auction);

    assertEquals(endTime.plusSeconds(60), auction.getEndTime());
    assertEquals(1, auction.getExtendedCount());
  }

  @Test
  void apply_shouldNotExtendAuctionOutsideThreshold() {
    LocalDateTime endTime = LocalDateTime.ofInstant(NOW.plusSeconds(31), ZONE);
    Auction auction = new Auction(1, 2, endTime, 1000);
    auction.start();

    new AntiSnipeService(Clock.fixed(NOW, ZONE)).apply(auction);

    assertEquals(endTime, auction.getEndTime());
    assertEquals(0, auction.getExtendedCount());
  }

  @Test
  void apply_shouldNotExtendWhenNullEndTime() {
    // Auction voi endTime null - dung constructor day du voi endTime = null
    Auction auction =
        new Auction(
            1,
            1,
            1,
            null,
            AuctionStatus.RUNNING,
            LocalDateTime.now(),
            null,
            1000L,
            0,
            0,
            LocalDateTime.now(),
            LocalDateTime.now());

    new AntiSnipeService(Clock.fixed(NOW, ZONE)).apply(auction);

    // Khong co extension nao xay ra khi endTime null
    assertNull(auction.getEndTime());
    assertEquals(0, auction.getExtendedCount());
  }

  @Test
  void apply_shouldNotExtendWhenMaxExtensionsReached() {
    // Tao auction voi extendedCount da bang MAX_EXTENSIONS = 5
    LocalDateTime endTime = LocalDateTime.ofInstant(NOW.plusSeconds(20), ZONE);
    Auction auction =
        new Auction(
            1,
            1,
            1,
            null,
            AuctionStatus.RUNNING,
            LocalDateTime.now(),
            endTime,
            1000L,
            5,
            0,
            LocalDateTime.now(),
            LocalDateTime.now());

    new AntiSnipeService(Clock.fixed(NOW, ZONE)).apply(auction);

    // Khong co extension nao vi da dat MAX
    assertEquals(endTime, auction.getEndTime());
    assertEquals(5, auction.getExtendedCount());
  }

  @Test
  void apply_shouldNotExtendWhenAuctionAlreadyExpired() {
    // Auction het han (secondsLeft <= 0)
    LocalDateTime endTime = LocalDateTime.ofInstant(NOW.minusSeconds(10), ZONE);
    Auction auction = new Auction(1, 2, endTime, 1000);
    auction.start();

    new AntiSnipeService(Clock.fixed(NOW, ZONE)).apply(auction);

    assertEquals(endTime, auction.getEndTime());
    assertEquals(0, auction.getExtendedCount());
  }
}
