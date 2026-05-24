package app.client.controllers;

import app.client.manager.ClientNotificationCenter;
import app.client.manager.ClientRequestService;
import app.client.manager.NavigationManager;
import app.client.manager.UserManager;
import app.client.store.AuctionStore;
import app.client.store.ItemStore;
import app.client.utils.AlertUtils;
import app.client.utils.LoadingButton;
import app.common.dto.*;
import app.common.enums.ItemType;
import app.common.enums.View;
import app.common.models.Item;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
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

public class AdminDashboardController implements Cleanable {
  private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);
  private final DecimalFormat currencyFormat = new DecimalFormat("#,###");
  private final DateTimeFormatter dateTimeFormatter =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

  private final ClientRequestService requests = ClientRequestService.getInstance();
  private final ClientNotificationCenter notifications = ClientNotificationCenter.getInstance();

  @FXML private Label adminNameLabel;
  @FXML private TextField auctionSearchField;
  @FXML private TextField userSearchField;

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

  // Users Table
  @FXML private TableView<UserDto> userTableView;
  @FXML private TableColumn<UserDto, Integer> colUserId;
  @FXML private TableColumn<UserDto, String> colUserName;
  @FXML private TableColumn<UserDto, String> colUserAccount;
  @FXML private TableColumn<UserDto, String> colUserBalance;
  @FXML private TableColumn<UserDto, String> colUserRole;

  private final List<AuctionPreview> masterAuctions = new ArrayList<>();
  private final List<UserDto> masterUsers = new ArrayList<>();

  private boolean actionLoading = false;
  private Button currentLoadingButton;
  private Runnable stopActionLoading = () -> {};

  // Listeners
  private final Runnable auctionsListener = () -> Platform.runLater(this::loadAuctionsData);
  private final Consumer<List<UserDto>> usersListener =
      users -> Platform.runLater(() -> loadUsersData(users));
  private final Consumer<String> messageListener =
      msg -> Platform.runLater(() -> handleIncomingMessage(msg));

  @FXML
  public void initialize() {
    setupGreeter();
    setupAuctionsTable();
    setupUsersTable();
    setupForm();
    setupSearchFilters();

    // Add listeners
    notifications.addUpdateListener(auctionsListener);
    notifications.addUserListListener(usersListener);
    notifications.addMessageListener(messageListener);

    // Initial fetch
    refreshAuctions();
    refreshUsers();
  }

  private void setupGreeter() {
    var user = UserManager.getInstance().getCurrentUser();
    if (user != null) {
      adminNameLabel.setText("Admin: " + user.getName());
    } else {
      adminNameLabel.setText("Admin: Administrator");
    }
  }

  private void setupAuctionsTable() {
    colAuctionId.setCellValueFactory(
        data -> new SimpleObjectProperty<>(data.getValue().auctionId()));
    colItemName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().itemName()));

    colItemType.setCellValueFactory(
        data -> {
          Item item = ItemStore.getInstance().getItem(data.getValue().itemId());
          String typeStr =
              (item != null && item.getType() != null) ? item.getType().name() : "OTHER";
          return new SimpleStringProperty(typeStr);
        });

    colStartingPrice.setCellValueFactory(
        data -> {
          Item item = ItemStore.getInstance().getItem(data.getValue().itemId());
          long price = (item != null) ? item.getStartingPrice() : 0;
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

  private void setupUsersTable() {
    colUserId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().id()));
    colUserName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
    colUserAccount.setCellValueFactory(
        data -> new SimpleStringProperty(data.getValue().account().username()));
    colUserBalance.setCellValueFactory(
        data -> {
          BigDecimal bal = data.getValue().wallet().availableBalance();
          return new SimpleStringProperty(bal != null ? currencyFormat.format(bal) + " đ" : "0 đ");
        });
    colUserRole.setCellValueFactory(
        data ->
            new SimpleStringProperty(
                data.getValue().account().role() != null
                    ? data.getValue().account().role().name()
                    : ""));
  }

  private void setupForm() {
    typeComboBox.getItems().setAll(ItemType.values());
    typeComboBox.getSelectionModel().selectFirst();
    handleClearForm();
  }

  private void setupSearchFilters() {
    auctionSearchField.textProperty().addListener((obs, oldVal, newVal) -> filterAuctions(newVal));
    userSearchField.textProperty().addListener((obs, oldVal, newVal) -> filterUsers(newVal));
  }

  private void autofillForm(AuctionPreview summary) {
    Item item = ItemStore.getInstance().getItem(summary.itemId());
    nameField.setText(summary.itemName());
    descriptionField.setText(item != null ? item.getDescription() : "");
    typeComboBox.setValue((item != null && item.getType() != null) ? item.getType() : ItemType.ART);

    // Duration
    // Calculate duration in minutes if possible
    if (summary.startTime() != null && summary.endTime() != null) {
      long durationMins =
          java.time.Duration.between(summary.startTime(), summary.endTime()).toMinutes();
      durationField.setText(String.valueOf(durationMins));
    } else {
      durationField.setText("60");
    }

    startingPriceField.setText(String.valueOf(item != null ? item.getStartingPrice() : 0));
    stepPriceField.setText(String.valueOf(item != null ? item.getStepPrice() : 0));

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
    masterAuctions.clear();
    masterAuctions.addAll(AuctionStore.getInstance().getAuctionPreviews());
    filterAuctions(auctionSearchField.getText());
  }

  private void loadUsersData(List<UserDto> users) {
    masterUsers.clear();
    if (users != null) {
      masterUsers.addAll(users);
    }
    filterUsers(userSearchField.getText());
  }

  private void filterAuctions(String query) {
    if (query == null || query.isBlank()) {
      auctionTableView.getItems().setAll(masterAuctions);
      return;
    }
    String keyword = query.toLowerCase().trim();
    List<AuctionPreview> filtered =
        masterAuctions.stream()
            .filter(
                a ->
                    (a.itemName() != null && a.itemName().toLowerCase().contains(keyword))
                        || String.valueOf(a.auctionId()).contains(keyword))
            .toList();
    auctionTableView.getItems().setAll(filtered);
  }

  private void filterUsers(String query) {
    if (query == null || query.isBlank()) {
      userTableView.getItems().setAll(masterUsers);
      return;
    }
    String keyword = query.toLowerCase().trim();
    List<UserDto> filtered =
        masterUsers.stream()
            .filter(
                u ->
                    (u.name() != null && u.name().toLowerCase().contains(keyword))
                        || (u.account().username() != null
                            && u.account().username().toLowerCase().contains(keyword))
                        || String.valueOf(u.id()).contains(keyword))
            .toList();
    userTableView.getItems().setAll(filtered);
  }

  // Refresh triggers
  private void refreshAuctions() {
    try {
      requests.fetchAuctionSummaries();
    } catch (IOException e) {
      logger.error("Failed to refresh auctions", e);
    }
  }

  private void refreshUsers() {
    try {
      requests.fetchUserList();
    } catch (IOException e) {
      logger.error("Failed to refresh user list", e);
    }
  }

  @FXML
  public void handleRefreshAuctions(ActionEvent event) {
    refreshAuctions();
  }

  @FXML
  public void handleRefreshUsers(ActionEvent event) {
    refreshUsers();
  }

  @FXML
  public void handleLogout(ActionEvent event) {
    UserManager.getInstance().setCurrentUser(null);
    NavigationManager.getInstance().navigateTo(View.LOGIN);
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
    if (actionLoading) return;
    if (!requests.isConnected()) {
      AlertUtils.showError("Mất kết nối", "Mất kết nối tới máy chủ.");
      return;
    }
    try {
      CreateAuctionRequest request = buildCreateRequest();
      if (request == null) return;

      currentLoadingButton = LoadingButton.fromEvent(event);
      setActionLoading(true);
      requests.createAuction(request);
    } catch (IOException e) {
      setActionLoading(false);
      AlertUtils.showError("Lỗi kết nối", "Server không phản hồi.");
    }
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

  private CreateAuctionRequest buildCreateRequest() {
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

      return new CreateAuctionRequest(
          name, desc, startingPrice, stepPrice, type, durationMins, startDateTime);
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
    notifications.removeUpdateListener(MasterAuctionsListener());
    notifications.removeUserListListener(usersListener);
    notifications.removeMessageListener(messageListener);
  }

  // Helper method to keep naming clean
  private Runnable MasterAuctionsListener() {
    return auctionsListener;
  }
}
