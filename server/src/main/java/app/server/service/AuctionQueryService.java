package app.server.service;

import app.common.dto.AuctionSummary;
import app.common.enums.AuctionStatus;
import app.common.mapper.DtoMapper;
import java.util.List;
import java.util.function.Predicate;

/** Read-only query helpers for auction snapshots and summaries. */
public class AuctionQueryService {

  public List<AuctionSummary> toAuctionSummaries(List<AuctionSnapshot> snapshots) {
    return snapshots.stream()
        .map(snapshot -> DtoMapper.toAuctionSummary(snapshot.auction(), snapshot.item()))
        .toList();
  }

  public List<AuctionSnapshot> filterHistorySnapshots(
      List<AuctionSnapshot> snapshots, Predicate<AuctionSnapshot> filter) {
    return snapshots.stream()
        .filter(snapshot -> isHistoryStatus(snapshot.auction().getStatus()))
        .filter(filter)
        .toList();
  }

  private boolean isHistoryStatus(AuctionStatus status) {
    return status == AuctionStatus.FINISHED
        || status == AuctionStatus.PAID
        || status == AuctionStatus.CANCELED;
  }
}
