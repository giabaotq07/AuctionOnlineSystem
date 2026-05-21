package app.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.common.models.Auction;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

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
}
