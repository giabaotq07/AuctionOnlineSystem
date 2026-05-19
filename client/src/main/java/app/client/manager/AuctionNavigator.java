package app.client.manager;

import app.client.Client;
import app.client.controllers.LiveController;
import app.client.utils.AlertUtils;
import app.common.dto.AuctionDetail;
import app.common.dto.AuctionDetailRequest;
import app.common.dto.AuctionDetailResponse;
import app.common.dto.AuctionSummary;
import app.common.enums.PacketType;
import app.common.enums.View;
import app.common.models.PacketReq;
import app.common.observer.PacketListener;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Opens auction details and routes to the live auction view. */
public final class AuctionNavigator {
  private static final Logger logger = LoggerFactory.getLogger(AuctionNavigator.class);
  private static volatile AuctionNavigator instance;

  private final Client client = Client.getInstance();
  private final Map<Integer, AuctionDetail> detailCache = new ConcurrentHashMap<>();
  private final PacketListener<AuctionDetailResponse> detailHandler;
  private Integer pendingAuctionId;

  private AuctionNavigator() {
    detailHandler =
        (response, success, message) ->
            Platform.runLater(() -> handleDetailResponse(response, success, message));
    //    client.subscribe(PacketType.FETCH_AUCTION_DETAIL, AuctionDetailResponse.class,
    // detailHandler);
  }

  /** getInstance. */
  public static AuctionNavigator getInstance() {
    if (instance == null) {
      synchronized (AuctionNavigator.class) {
        if (instance == null) {
          instance = new AuctionNavigator();
        }
      }
    }
    return instance;
  }

  /** open. */
  public void open(AuctionSummary summary) {
    if (summary == null) {
      return;
    }
    if (!client.isConnected()) {
      AlertUtils.showError("Mất kết nối", "Vui lòng kết nối lại!");
      return;
    }
    if (UserSession.getInstance().getCurrentUser() == null) {
      AlertUtils.showError("Chưa đăng nhập", "Bạn phải đăng nhập!");
      NavigationManager.getInstance().navigateTo(View.LOGIN);
      return;
    }
    requestDetail(summary);
  }

  private void requestDetail(AuctionSummary summary) {
    pendingAuctionId = summary.auctionId();
    try {
      AuctionDetailRequest request =
          new AuctionDetailRequest(
              summary.auctionId(), getCachedDetailVersion(summary.auctionId()));
      client.sendRequest(PacketReq.of(PacketType.FETCH_AUCTION_DETAIL, request));
    } catch (IOException e) {
      pendingAuctionId = null;
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
      logger.error("Failed to request auction detail", e);
    }
  }

  private void handleDetailResponse(
      AuctionDetailResponse response, boolean success, String message) {
    if (!success) {
      if (pendingAuctionId != null) {
        pendingAuctionId = null;
        AlertUtils.showError("Lỗi", message);
      }
      return;
    }
    AuctionDetail detail = resolveDetail(response);
    if (detail == null) {
      return;
    }
    cacheDetail(detail);
    if (pendingAuctionId != null && pendingAuctionId == detail.auctionId()) {
      pendingAuctionId = null;
      navigateToLive(detail);
    }
  }

  private AuctionDetail resolveDetail(AuctionDetailResponse response) {
    if (response == null) {
      return null;
    }
    if (response.detail() != null) {
      return response.detail();
    }
    if (response.notModified()) {
      return getCachedDetail(response.auctionId());
    }
    return null;
  }

  /** cacheDetail. */
  public void cacheDetail(AuctionDetail detail) {
    if (detail == null) {
      return;
    }
    detailCache.put(detail.auctionId(), detail);
  }

  /** getCachedDetail. */
  public AuctionDetail getCachedDetail(int auctionId) {
    return detailCache.get(auctionId);
  }

  private int getCachedDetailVersion(int auctionId) {
    AuctionDetail detail = detailCache.get(auctionId);
    return detail == null ? -1 : detail.version();
  }

  private void navigateToLive(AuctionDetail detail) {
    NavigationManager.getInstance()
        .navigateTo(
            View.LIVE,
            controller -> {
              if (controller instanceof LiveController liveController) {
                liveController.setAuction(detail);
              }
            });
  }
}
