package app.server.service;

import app.common.enums.AuctionStatus;
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
  private static volatile AuctionScheduler instance;
  private final ScheduledExecutorService scheduler;
  private AuctionService auctionService;
  private AuctionQueryService auctionQueryService;

  private AuctionScheduler() {
    this.scheduler = Executors.newScheduledThreadPool(2);
  }

  public static synchronized AuctionScheduler getInstance() {
    if (instance == null) {
      instance = new AuctionScheduler();
    }
    return instance;
  }

  public void init(AuctionService auctionService, AuctionQueryService auctionQueryService) {
    this.auctionService = auctionService;
    this.auctionQueryService = auctionQueryService;
    scheduleExistingOpenAuctions();
  }

  private void scheduleExistingOpenAuctions() {
    if (auctionQueryService == null) {
      return;
    }
    List<app.common.models.Auction> auctions = auctionQueryService.getAuctions();
    for (app.common.models.Auction auction : auctions) {
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
                Server.broadcastAuctionList(auctionQueryService);
              }
            }
          } catch (Exception e) {
            logger.error("Error starting auction {}", auctionId, e);
          }
        },
        delay,
        TimeUnit.MILLISECONDS);
  }

  public void shutdown() {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
  }
}
