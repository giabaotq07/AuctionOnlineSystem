package app.controllers;

import app.Client;
import app.DataStore;
import app.controllers.manager.NavigationManager;
import app.dto.DepositRequest;
import app.dto.WalletUpdateResponse;
import app.enums.PacketType;
import app.enums.View;
import app.models.PacketReq;
import app.models.User;
import app.models.Wallet;
import app.network.PacketListener;
import app.utils.AlertUtils;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** DepositController. */
public class DepositController implements Cleanable {
  private static final Logger logger = LoggerFactory.getLogger(DepositController.class);
  @FXML private TextField depositAmountField;
  @FXML private Label totalBalanceLabel;
  @FXML private Label availableBalanceLabel;
  private final Client client = Client.getInstance();
  private PacketListener<WalletUpdateResponse> walletUpdateHandler;
  private BigDecimal pendingDepositAmount;
  private final DecimalFormat currencyFormat = new DecimalFormat("#,###");

  /** Member. */
  @FXML
  public void initialize() {
    setupWalletListener();
    updateBalanceLabels(client.getCurrentUser());
  }

  private void setupWalletListener() {
    walletUpdateHandler =
        (response, success, message) ->
            Platform.runLater(
                () -> {
                  if (!success) {
                    pendingDepositAmount = null;
                    AlertUtils.showError("Ví", message);
                    return;
                  }
                  if (response != null && response.user() != null) {
                    DataStore.getInstance().updateCurrentUser(response.user());
                  }
                  updateBalanceLabels(client.getCurrentUser());
                  if (pendingDepositAmount != null) {
                    AlertUtils.showInfo(
                        "Nạp tiền thành công",
                        "Đã nạp: " + formatCurrency(pendingDepositAmount) + " đ");
                    pendingDepositAmount = null;
                  }
                });
    client.subscribe(PacketType.WALLET_UPDATE, walletUpdateHandler);
  }

  private void updateBalanceLabels(User user) {
    if (totalBalanceLabel == null || availableBalanceLabel == null) {
      return;
    }
    if (user == null) {
      totalBalanceLabel.setText("Tổng số dư: 0 đ");
      availableBalanceLabel.setText("Số dư khả dụng: 0 đ");
      return;
    }
    Wallet wallet = user.getWallet();
    totalBalanceLabel.setText("Tổng số dư: " + formatCurrency(wallet.getTotalBalance()) + " đ");
    availableBalanceLabel.setText(
        "Số dư khả dụng: " + formatCurrency(wallet.getAvailableBalance()) + " đ");
  }

  private String formatCurrency(BigDecimal amount) {
    if (amount == null) {
      return "0";
    }
    return currencyFormat.format(amount);
  }

  /** Member. */
  @FXML
  public void handleDeposit(ActionEvent event) {
    if (!client.isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server!");
      return;
    }
    if (client.getCurrentUser() == null) {
      AlertUtils.showError("Lỗi", "Bạn phải đăng nhập để nạp tiền!");
      return;
    }
    if (depositAmountField == null) {
      return;
    }
    BigDecimal amount;
    try {
      amount = new BigDecimal(depositAmountField.getText().trim());
    } catch (NumberFormatException e) {
      AlertUtils.showError("Lỗi", "Số tiền nạp không hợp lệ");
      return;
    }
    if (amount.signum() <= 0) {
      AlertUtils.showError("Lỗi", "Số tiền nạp phải lớn hơn 0");
      return;
    }
    try {
      pendingDepositAmount = amount;
      client.sendRequest(PacketReq.of(PacketType.DEPOSIT, new DepositRequest(amount)));
      depositAmountField.clear();
    } catch (Exception e) {
      pendingDepositAmount = null;
      logger.error("Failed to deposit", e);
      AlertUtils.showError("Lỗi", "Không thể nạp tiền.");
    }
  }

  /** Member. */
  @FXML
  public void handleBack(ActionEvent event) {
    NavigationManager.getInstance().navigateTo(View.UI);
  }

  @Override
  public void cleanup() {
    if (walletUpdateHandler != null) {
      client.unsubscribe(PacketType.WALLET_UPDATE, walletUpdateHandler);
    }
  }
}
