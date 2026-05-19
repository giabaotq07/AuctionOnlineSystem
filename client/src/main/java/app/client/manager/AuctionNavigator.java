package app.client.manager;

import app.client.controllers.LiveController;
import app.client.store.AuctionStore;
import app.client.utils.AlertUtils;
import app.common.dto.AuctionDetail;
import app.common.dto.AuctionSummary;
import app.common.enums.PacketType;
import app.common.enums.View;
import app.common.mapper.DtoMapper;
import app.common.models.PacketRes;
import java.io.IOException;
import java.util.function.Consumer;
import javafx.application.Platform;

/** Opens auction details after ensuring the full model is cached. */
public final class AuctionNavigator {
  private static volatile AuctionNavigator instance;

  private final ClientRequestService requests = ClientRequestService.getInstance();
  private final ClientNotificationCenter notifications = ClientNotificationCenter.getInstance();

  private AuctionNavigator() {}

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
    AuctionStore.getInstance().addAuction(DtoMapper.toAuction(summary));
    AuctionDetail detail = AuctionStore.getInstance().getAuctionDetail(summary.auctionId());
    if (detail != null) {
      navigateToLive(detail);
      return;
    }
    if (!requests.isConnected()) {
      AlertUtils.showError("Mất kết nối", "Vui lòng kết nối lại!");
      return;
    }
    if (UserManager.getInstance().getCurrentUser() == null) {
      AlertUtils.showError("Chưa đăng nhập", "Bạn phải đăng nhập!");
      NavigationManager.getInstance().navigateTo(View.LOGIN);
      return;
    }
    requestAndOpen(summary.auctionId());
  }

  private void requestAndOpen(int auctionId) {
    @SuppressWarnings("unchecked")
    Consumer<PacketRes>[] listenerRef = new Consumer[1];
    listenerRef[0] =
        packet -> {
          if (packet == null || packet.getType() != PacketType.FETCH_AUCTION_DETAIL) {
            return;
          }
          Platform.runLater(
              () -> {
                notifications.removeListener(listenerRef[0]);
                AuctionDetail detail = AuctionStore.getInstance().getAuctionDetail(auctionId);
                if (detail != null) {
                  navigateToLive(detail);
                } else {
                  AlertUtils.showError("Lỗi", packet.getMessage());
                }
              });
        };
    notifications.addListener(listenerRef[0]);
    try {
      requests.fetchAuctionDetail(auctionId, -1);
    } catch (IOException e) {
      notifications.removeListener(listenerRef[0]);
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
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
