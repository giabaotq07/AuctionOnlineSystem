package app.client.controllers;

import app.client.Client;
import app.client.manager.NavigationManager;
import app.client.manager.UserSession;
import app.client.utils.AlertUtils;
import app.common.dto.DepositRequest;
import app.common.enums.PacketType;
import app.common.enums.View;
import app.common.models.PacketReq;
import app.common.models.User;
import app.common.models.Wallet;
import java.math.BigDecimal;
import java.text.DecimalFormat;
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
  private final DecimalFormat currencyFormat = new DecimalFormat("#,###");

  /** Member. */
  @FXML
  public void initialize() {
    updateBalanceLabels(UserSession.getInstance().getCurrentUser());
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
    if (UserSession.getInstance().getCurrentUser() == null) {
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
      client.sendRequest(PacketReq.of(PacketType.DEPOSIT, new DepositRequest(amount)));
      depositAmountField.clear();
    } catch (Exception e) {
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
  public void cleanup() {}
}
