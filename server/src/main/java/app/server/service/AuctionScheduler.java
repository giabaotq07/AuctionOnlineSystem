package app.server.service;

import app.common.dto.AuctionPaidNoticeResponse;
import app.common.dto.AuctionSummariesResponse;
import app.common.dto.WalletUpdateResponse;
import app.common.enums.AuctionStatus;
import app.common.enums.PacketType;
import app.common.mapper.DtoMapper;
import app.common.models.Auction;
import app.common.models.Bid;
import app.common.models.User;
import app.common.protocol.PacketRes;
import app.server.network.Server;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
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
  private UserService userService;

  private AuctionScheduler() {
    this.scheduler = Executors.newScheduledThreadPool(10);
  }

  public static synchronized AuctionScheduler getInstance() {
    if (instance == null) {
      instance = new AuctionScheduler();
    }
    return instance;
  }

  public void init(AuctionService auctionService, UserService userService) {
    this.auctionService = auctionService;
    this.userService = userService;
    scheduleExistingOpenAuctions();
  }

  private void scheduleExistingOpenAuctions() {
    if (auctionService == null) return;
    List<AuctionSnapshot> snapshots = auctionService.getAuctions();
    for (AuctionSnapshot snapshot : snapshots) {
      Auction auction = snapshot.auction();
      if (auction.getStatus() == AuctionStatus.OPEN && auction.getStartTime() != null) {
        scheduleStart(auction.getId(), auction.getStartTime());
      }
      if (auction.getEndTime() != null
          && auction.getStatus() != AuctionStatus.FINISHED
          && auction.getStatus() != AuctionStatus.PAID
          && auction.getStatus() != AuctionStatus.CANCELED) {
        scheduleFinish(auction.getId(), auction.getEndTime());
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

  public void scheduleFinish(int auctionId, LocalDateTime endTime) {
    long delay = ChronoUnit.MILLIS.between(LocalDateTime.now(), endTime);
    if (delay < 0) delay = 0;

    scheduler.schedule(
        () -> {
          try {
            if (auctionService != null) {
              boolean updated = auctionService.completeAuctionIfExpired(auctionId);
              if (updated) {
                logger.info("Auction {} finished automatically.", auctionId);
                notifyPaymentIfNeeded(auctionId);
                broadcastAuctionList();
              }
            }
          } catch (Exception e) {
            logger.error("Error finishing auction {}", auctionId, e);
          }
        },
        delay,
        TimeUnit.MILLISECONDS);
  }

  public void notifyPaymentIfNeeded(int auctionId) {
    if (auctionService == null) {
      return;
    }
    try {
      AuctionSnapshot snapshot = auctionService.getAuction(auctionId);
      if (snapshot.auction().getStatus() != AuctionStatus.PAID) {
        return;
      }
      Integer winnerId = snapshot.auction().getWinnerId();
      if (winnerId == null) {
        return;
      }
      Optional<Bid> highestBid = auctionService.findHighestBid(auctionId);
      BigDecimal amount =
          highestBid.map(bid -> BigDecimal.valueOf(bid.getAmount())).orElse(BigDecimal.ZERO);
      if (amount.signum() <= 0) {
        return;
      }
      String auctionName =
          snapshot.item() == null ? "Phiên #" + auctionId : snapshot.item().getName();
      AuctionPaidNoticeResponse sellerNotice =
          new AuctionPaidNoticeResponse(auctionId, auctionName, amount, "SELLER");
      AuctionPaidNoticeResponse winnerNotice =
          new AuctionPaidNoticeResponse(auctionId, auctionName, amount, "WINNER");
      Server.sendToUser(
          snapshot.auction().getSellerId(),
          PacketRes.of(PacketType.AUCTION_PAID_NOTICE, "hello", sellerNotice));
      Server.sendToUser(
          winnerId, PacketRes.of(PacketType.AUCTION_PAID_NOTICE, "hello", winnerNotice));
      sendWalletUpdate(snapshot.auction().getSellerId());
      sendWalletUpdate(winnerId);
    } catch (Exception ex) {
      logger.error("Failed to notify payment for auction {}", auctionId, ex);
    }
  }

  private void sendWalletUpdate(int userId) {
    if (userService == null) {
      return;
    }
    try {
      User user = userService.getById(userId);
      WalletUpdateResponse response = new WalletUpdateResponse(DtoMapper.toUserData(user));
      Server.sendToUser(userId, PacketRes.of(PacketType.WALLET_UPDATE, "OK", response));
    } catch (Exception ex) {
      logger.warn("Failed to send wallet update to user {}", userId, ex);
    }
  }

  private void broadcastAuctionList() {
    try {
      AuctionSummariesResponse summariesResponse =
          new AuctionSummariesResponse(
              auctionService.getAuctions().stream()
                  .map(snapshot -> DtoMapper.toAuctionSummary(snapshot.auction(), snapshot.item()))
                  .toList());
      Server.broadcast(
          PacketRes.of(PacketType.FETCH_AUCTION_SUMMARIES, "okd", summariesResponse), -1);
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
