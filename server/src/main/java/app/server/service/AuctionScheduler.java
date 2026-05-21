package app.server.service;

import app.common.dto.AuctionSummariesResponse;
import app.common.enums.AuctionStatus;
import app.common.enums.PacketType;
import app.common.mapper.DtoMapper;
import app.common.models.Auction;
import app.common.protocol.PacketRes;
import app.server.network.Server;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuctionScheduler {
  private static final Logger logger = LoggerFactory.getLogger(AuctionScheduler.class);
  private static AuctionScheduler instance;
  private final ScheduledExecutorService scheduler;
  private AuctionService auctionService;

  private AuctionScheduler() {
    this.scheduler = Executors.newScheduledThreadPool(10);
  }

  public static synchronized AuctionScheduler getInstance() {
    if (instance == null) {
      instance = new AuctionScheduler();
    }
    return instance;
  }

  public void init(AuctionService auctionService) {
    this.auctionService = auctionService;
    scheduleExistingOpenAuctions();
  }

  private void scheduleExistingOpenAuctions() {
    if (auctionService == null) {
      return;
    }
    List<AuctionSnapshot> snapshots = auctionService.getAuctions();
    for (AuctionSnapshot snapshot : snapshots) {
      Auction auction = snapshot.auction();
      if (auction.getStatus() == AuctionStatus.OPEN && auction.getStartTime() != null) {
        scheduleStart(auction.getId(), auction.getStartTime());
      }
    }
  }

  public void scheduleStart(int auctionId, LocalDateTime startTime) {
    long delay = ChronoUnit.MILLIS.between(LocalDateTime.now(), startTime);
    if (delay < 0) delay = 0;

    scheduler.schedule(
        () -> {
          try {
            if (auctionService != null) {
              boolean updated = auctionService.startOpenAuction(auctionId);
              if (updated) {
                logger.info("Auction {} started automatically.", auctionId);
                broadcastAuctionList();
              }
            }
          } catch (Exception e) {
            logger.error("Error starting auction {}", auctionId, e);
          }
        },
        delay,
        TimeUnit.MILLISECONDS);
  }

  private void broadcastAuctionList() {
    try {
      AuctionSummariesResponse summariesResponse =
          new AuctionSummariesResponse(
              auctionService.getAuctions().stream()
                  .map(snapshot -> DtoMapper.toAuctionSummary(snapshot.auction(), snapshot.item()))
                  .toList());
      Server.broadcast(
          PacketRes.of(PacketType.AUCTION_SUMMARIES_UPDATED, "OK", summariesResponse), -1);
    } catch (Exception ex) {
      logger.error("Failed to broadcast updated auction list", ex);
    }
  }

  public void shutdown() {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
  }
}
