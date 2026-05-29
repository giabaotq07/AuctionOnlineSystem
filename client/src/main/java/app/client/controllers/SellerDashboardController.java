package app.client.controllers;

import app.client.manager.ClientNotificationCenter;
import app.client.manager.ClientRequestService;
import app.client.manager.NavigationManager;
import app.client.manager.UserManager;
import app.client.store.AuctionStore;
import app.client.store.ItemStore;
import app.client.utils.AlertUtils;
import app.client.utils.LoadingButton;
import app.common.dto.AuctionPreview;
import app.common.dto.UpdateAuctionRequest;
import app.common.enums.AuctionStatus;
import app.common.enums.ItemType;
import app.common.enums.View;
import app.common.models.Item;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SellerDashboardController implements Cleanable {
  private static final Logger logger = LoggerFactory.getLogger(SellerDashboardController.class);
  private final java.text.DecimalFormat currencyFormat = new java.text.DecimalFormat("#,###");
  private final DateTimeFormatter dateTimeFormatter =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

  private final ClientRequestService requests = ClientRequestService.getInstance();
  private final ClientNotificationCenter notifications = ClientNotificationCenter.getInstance();

  @FXML private Label sellerNameLabel;
  @FXML private TextField auctionSearchField;
  @FXML private ComboBox<String> typeFilterComboBox;

  // Auctions Table
  @FXML private TableView<AuctionPreview> auctionTableView;
  @FXML private TableColumn<AuctionPreview, Integer> colAuctionId;
  @FXML private TableColumn<AuctionPreview, String> colItemName;
  @FXML private TableColumn<AuctionPreview, String> colItemType;
  @FXML private TableColumn<AuctionPreview, String> colStartingPrice;
  @FXML private TableColumn<AuctionPreview, String> colHighestBid;
  @FXML private TableColumn<AuctionPreview, String> colStartTime;
  @FXML private TableColumn<AuctionPreview, String> colStatus;

  // Detail Form Inputs
  @FXML private TextField nameField;
  @FXML private TextArea descriptionField;
  @FXML private ComboBox<ItemType> typeComboBox;
  @FXML private TextField durationField;
  @FXML private TextField startingPriceField;
  @FXML private TextField stepPriceField;
  @FXML private DatePicker startDatePicker;
  @FXML private TextField startTimeField;

  private final List<AuctionPreview> masterAuctions = new ArrayList<>();

  private boolean actionLoading = false;
  private Button currentLoadingButton;
  private Runnable stopActionLoading = () -> {};

  // Listeners
  private final Runnable auctionsListener = () -> Platform.runLater(this::loadAuctionsData);
  private final Consumer<String> messageListener =
      msg -> Platform.runLater(() -> handleIncomingMessage(msg));

  @FXML
  public void initialize() {
    setupGreeter();
    setupAuctionsTable();
    setupForm();
    setupSearchFilters();

    // Add listeners
    notifications.addUpdateListener(auctionsListener);
    notifications.addMessageListener(messageListener);

    // Load master list immediately on load if there's cached data!
    loadAuctionsData();

    // Initial fetch to get latest from server
    refreshAuctions();
  }

  private void setupGreeter() {
    var user = UserManager.getInstance().getCurrentUser();
    if (user != null) {
      sellerNameLabel.setText("Người bán: " + user.getName());
    } else {
      sellerNameLabel.setText("Người bán: ");
    }
  }

  private void setupAuctionsTable() {
    colAuctionId.setCellValueFactory(
        data -> new SimpleObjectProperty<>(data.getValue().auctionId()));
    colItemName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().itemName()));

    colItemType.setCellValueFactory(
        data -> {
          ItemType type = data.getValue().itemType();
          String typeStr = (type != null) ? type.name() : "OTHER";
          return new SimpleStringProperty(typeStr);
        });

    colStartingPrice.setCellValueFactory(
        data -> {
          long price = data.getValue().startingPrice();
          return new SimpleStringProperty(currencyFormat.format(price) + " đ");
        });

    colHighestBid.setCellValueFactory(
        data ->
            new SimpleStringProperty(currencyFormat.format(data.getValue().highestBid()) + " đ"));

    colStartTime.setCellValueFactory(
        data -> {
          LocalDateTime time = data.getValue().startTime();
          return new SimpleStringProperty(time != null ? time.format(dateTimeFormatter) : "");
        });

    colStatus.setCellValueFactory(
        data ->
            new SimpleStringProperty(
                data.getValue().status() != null ? data.getValue().status().name() : ""));

    // Form autofill when row selected
    auctionTableView
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (newVal != null) {
                autofillForm(newVal);
              }
            });
  }

  private void setupForm() {
    typeComboBox.getItems().setAll(ItemType.values());
    typeComboBox.getSelectionModel().selectFirst();
    handleClearForm();
  }

  private void setupSearchFilters() {
    auctionSearchField.textProperty().addListener((obs, oldVal, newVal) -> filterAuctions(newVal));

    typeFilterComboBox.getItems().addAll("ALL", "ELECTRONICS", "ART", "VEHICLE");
    typeFilterComboBox.setValue("ALL");
    typeFilterComboBox.setOnAction(e -> filterAuctions(auctionSearchField.getText()));
  }

  private void autofillForm(AuctionPreview summary) {
    Item item = ItemStore.getInstance().getItem(summary.itemId());
    nameField.setText(summary.itemName());
    descriptionField.setText(item != null ? item.getDescription() : "");
    typeComboBox.setValue((summary.itemType() != null) ? summary.itemType() : ItemType.ART);

    // Duration
    // Calculate duration in minutes if possible
    if (summary.startTime() != null && summary.endTime() != null) {
      long durationMins =
          java.time.Duration.between(summary.startTime(), summary.endTime()).toMinutes();
      durationField.setText(String.valueOf(durationMins));
    } else {
      durationField.setText("60");
    }

    startingPriceField.setText(String.valueOf(summary.startingPrice()));
    stepPriceField.setText(String.valueOf(summary.stepPrice()));

    if (summary.startTime() != null) {
      startDatePicker.setValue(summary.startTime().toLocalDate());
      startTimeField.setText(summary.startTime().toLocalTime().format(timeFormatter));
    } else {
      startDatePicker.setValue(LocalDate.now());
      startTimeField.setText("12:00");
    }
  }

  // Master Data Loaders
  private void loadAuctionsData() {
    var user = UserManager.getInstance().getCurrentUser();
    if (user == null) {
      masterAuctions.clear();
      auctionTableView.getItems().clear();
      return;
    }
    int currentUserId = user.getId();
    masterAuctions.clear();

    // Only display auctions belonging to the currently logged in Seller using the UserPreview
    for (AuctionPreview summary : AuctionStore.getInstance().getAuctionPreviews()) {
      if (summary.seller() != null && summary.seller().userId() == currentUserId) {
        masterAuctions.add(summary);
      }
    }

    // Sort by status: OPEN -> RUNNING -> PAID -> FINISHED (end) -> CANCELED
    masterAuctions.sort(
        (left, right) -> {
          int scoreLeft = getStatusScore(left.status());
          int scoreRight = getStatusScore(right.status());
          if (scoreLeft != scoreRight) {
            return Integer.compare(scoreLeft, scoreRight);
          }
          return Integer.compare(right.auctionId(), left.auctionId());
        });

    filterAuctions(auctionSearchField.getText());
  }

  private int getStatusScore(AuctionStatus status) {
    if (status == null) return 99;
    return switch (status) {
      case OPEN -> 1;
      case RUNNING -> 2;
      case PAID -> 3;
      case FINISHED -> 4;
      case CANCELED -> 5;
    };
  }

  private void filterAuctions(String query) {
    String type = typeFilterComboBox.getValue() == null ? "ALL" : typeFilterComboBox.getValue();
    String keyword = (query == null) ? "" : query.toLowerCase().trim();
    List<AuctionPreview> filtered =
        masterAuctions.stream()
            .filter(
                a -> {
                  boolean matchesName =
                      (a.itemName() != null && a.itemName().toLowerCase().contains(keyword))
                          || String.valueOf(a.auctionId()).contains(keyword);
                  if (!matchesName) {
                    return false;
                  }
                  if ("ALL".equals(type)) {
                    return true;
                  }
                  return a.itemType() != null && a.itemType().name().equals(type);
                })
            .toList();
    auctionTableView.getItems().setAll(filtered);
  }

  // Refresh triggers
  private void refreshAuctions() {
    try {
      requests.fetchAuctionSummaries();
    } catch (IOException e) {
      logger.error("Failed to refresh auctions", e);
    }
  }

  @FXML
  public void handleRefreshAuctions(ActionEvent event) {
    refreshAuctions();
  }

  @FXML
  public void handleBackToMain(ActionEvent event) {
    NavigationManager.getInstance().navigateTo(View.UI);
  }

  @FXML
  public void handleClearForm() {
    nameField.clear();
    descriptionField.clear();
    typeComboBox.getSelectionModel().selectFirst();
    durationField.setText("60");
    startingPriceField.clear();
    stepPriceField.clear();
    startDatePicker.setValue(LocalDate.now());
    startTimeField.setText("12:00");
    auctionTableView.getSelectionModel().clearSelection();
  }

  // CRUD actions
  @FXML
  public void handleCreateAuction(ActionEvent event) {
    AlertUtils.showError("Không được phép", "Vui lòng tạo phiên mới tại màn Tạo phiên đấu giá.");
  }

  @FXML
  public void handleUpdateAuction(ActionEvent event) {
    if (actionLoading) return;
    if (!requests.isConnected()) {
      AlertUtils.showError("Mất kết nối", "Mất kết nối tới máy chủ.");
      return;
    }
    AuctionPreview selected = auctionTableView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      AlertUtils.showError("Chưa chọn phiên", "Vui lòng chọn phiên đấu giá cần cập nhật từ bảng.");
      return;
    }
    try {
      UpdateAuctionRequest request = buildUpdateRequest(selected);
      if (request == null) return;

      currentLoadingButton = LoadingButton.fromEvent(event);
      setActionLoading(true);
      requests.updateAuction(request);
    } catch (IOException e) {
      setActionLoading(false);
      AlertUtils.showError("Lỗi kết nối", "Server không phản hồi.");
    }
  }

  @FXML
  public void handleCancelAuction(ActionEvent event) {
    if (actionLoading) return;
    if (!requests.isConnected()) {
      AlertUtils.showError("Mất kết nối", "Mất kết nối tới máy chủ.");
      return;
    }
    AuctionPreview selected = auctionTableView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      AlertUtils.showError("Chưa chọn phiên", "Vui lòng chọn phiên đấu giá cần hủy.");
      return;
    }

    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Xác nhận hủy phiên");
    alert.setHeaderText("Bạn có chắc chắn muốn hủy phiên đấu giá #" + selected.auctionId() + "?");
    alert.setContentText(
        "Hành động này sẽ giải phóng tất cả số tiền bị đóng băng của người tham gia.");

    if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
      try {
        currentLoadingButton = LoadingButton.fromEvent(event);
        setActionLoading(true);
        requests.cancelAuction(selected.auctionId(), selected.version());
      } catch (IOException e) {
        setActionLoading(false);
        AlertUtils.showError("Lỗi kết nối", "Server không phản hồi.");
      }
    }
  }

  private UpdateAuctionRequest buildUpdateRequest(AuctionPreview selected) {
    String name = nameField.getText().trim();
    String desc = descriptionField.getText().trim();
    String startingStr = startingPriceField.getText().trim();
    String stepStr = stepPriceField.getText().trim();
    String durationStr = durationField.getText().trim();
    ItemType type = typeComboBox.getValue();
    LocalDate startDate = startDatePicker.getValue();
    String timeStr = startTimeField.getText().trim();

    if (name.isEmpty()
        || startingStr.isEmpty()
        || stepStr.isEmpty()
        || durationStr.isEmpty()
        || startDate == null
        || timeStr.isEmpty()) {
      AlertUtils.showError("Thiếu thông tin", "Vui lòng nhập đầy đủ các thông tin bắt buộc (*).");
      return null;
    }

    try {
      long startingPrice = Long.parseLong(startingStr);
      long stepPrice = Long.parseLong(stepStr);
      int durationMins = Integer.parseInt(durationStr);
      LocalTime startTime = LocalTime.parse(timeStr, timeFormatter);
      LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);

      return new UpdateAuctionRequest(
          selected.auctionId(),
          name,
          desc,
          startingPrice,
          stepPrice,
          type,
          durationMins,
          startDateTime,
          selected.version());
    } catch (NumberFormatException e) {
      AlertUtils.showError(
          "Sai định dạng số", "Giá khởi điểm, bước giá và thời lượng phải là số nguyên hợp lệ.");
      return null;
    } catch (DateTimeParseException e) {
      AlertUtils.showError(
          "Sai định dạng giờ", "Giờ bắt đầu phải tuân theo định dạng HH:mm (ví dụ: 14:30).");
      return null;
    }
  }

  // Handle Response Message
  private void handleIncomingMessage(String msg) {
    if (!actionLoading) return;
    setActionLoading(false);

    if (msg != null
        && (msg.toLowerCase().contains("thành công") || msg.toLowerCase().contains("ok"))) {
      AlertUtils.showInfo("Thành công", msg);
      handleClearForm();
      refreshAuctions();
    } else {
      AlertUtils.showError("Thất bại", msg != null ? msg : "Đã xảy ra lỗi không xác định.");
    }
  }

  private void setActionLoading(boolean loading) {
    actionLoading = loading;
    if (loading) {
      stopActionLoading = LoadingButton.show(currentLoadingButton);
    } else {
      stopActionLoading.run();
      stopActionLoading = () -> {};
    }
  }

  @Override
  public void cleanup() {
    notifications.removeUpdateListener(auctionsListener);
    notifications.removeMessageListener(messageListener);
  }
}
