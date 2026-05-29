package app.client.controllers;

import app.client.manager.ClientNotificationCenter;
import app.client.manager.ClientRequestService;
import app.client.manager.NavigationManager;
import app.client.manager.UserManager;
import app.client.store.AuctionStore;
import app.client.store.ItemStore;
import app.client.store.UserListStore;
import app.client.utils.AlertUtils;
import app.client.utils.LoadingButton;
import app.common.dto.*;
import app.common.enums.AuctionStatus;
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
  @FXML private DatePicker endDatePicker;
  @FXML private TextField endTimeField;

  // Users Table
  @FXML private TableView<UserDto> userTableView;
  @FXML private TableColumn<UserDto, Integer> colUserId;
  @FXML private TableColumn<UserDto, String> colUserName;
  @FXML private TableColumn<UserDto, String> colUserAccount;
  @FXML private TableColumn<UserDto, String> colUserBalance;
  @FXML private TableColumn<UserDto, String> colUserRole;
  @FXML private TableColumn<UserDto, String> colUserStatus;
  @FXML private TableColumn<UserDto, Void> colUserAction;

  private final List<AuctionPreview> masterAuctions = new ArrayList<>();

  private boolean actionLoading = false;
  private Button currentLoadingButton;
  private Runnable stopActionLoading = () -> {};
  private PendingAction pendingAction = PendingAction.NONE;

  private enum PendingAction {
    NONE,
    AUCTION,
    USER
  }

  // Listeners
  private final Runnable auctionsListener = () -> Platform.runLater(this::loadAuctionsData);
  private final Runnable usersListener = () -> Platform.runLater(this::loadUsersData);
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

    // Load master list immediately on load if there's cached data!
    loadAuctionsData();
    loadUsersData();

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
          var type = data.getValue().itemType();
          String typeStr = (type != null) ? type.name() : "OTHER";
          return new SimpleStringProperty(typeStr);
        });

    colStartingPrice.setCellValueFactory(
        data -> {
          long price = data.getValue().startingPrice();
          return new SimpleStringProperty(currencyFormat.format(price) + " $");
        });

    colHighestBid.setCellValueFactory(
        data ->
            new SimpleStringProperty(currencyFormat.format(data.getValue().highestBid()) + " $"));

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
        data -> {
          var acc = data.getValue().account();
          return new SimpleStringProperty(acc != null ? acc.username() : "");
        });
    colUserBalance.setCellValueFactory(
        data -> {
          var w = data.getValue().wallet();
          BigDecimal bal = w != null ? w.availableBalance() : null;
          return new SimpleStringProperty(bal != null ? currencyFormat.format(bal) + " $" : "0 $");
        });
    colUserRole.setCellValueFactory(
        data -> {
          var acc = data.getValue().account();
          return new SimpleStringProperty(
              acc != null && acc.role() != null ? acc.role().name() : "");
        });
    colUserStatus.setCellValueFactory(
        data -> new SimpleStringProperty(data.getValue().isBanned() ? "Bị cấm" : "Hoạt động"));
    colUserAction.setCellFactory(
        column ->
            new TableCell<>() {
              private final Button actionButton = new Button();

              {
                actionButton.setMaxWidth(Double.MAX_VALUE);
                actionButton.setOnAction(
                    event -> {
                      UserDto user = getTableView().getItems().get(getIndex());
                      handleToggleUserBan(user, event);
                    });
              }

              @Override
              protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                  setGraphic(null);
                  return;
                }
                UserDto user = getTableView().getItems().get(getIndex());
                boolean banned = user.isBanned();
                actionButton.setText(banned ? "Mở khóa" : "Cấm");
                actionButton
                    .getStyleClass()
                    .removeAll("success-button", "danger-button", "secondary-button");
                actionButton.getStyleClass().add(banned ? "success-button" : "danger-button");
                actionButton.setDisable(isCurrentUser(user));
                setGraphic(actionButton);
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
    userSearchField.textProperty().addListener((obs, oldVal, newVal) -> filterUsers(newVal));

    typeFilterComboBox.getItems().addAll("ALL", "ELECTRONICS", "ART", "VEHICLE");
    typeFilterComboBox.setValue("ALL");
    typeFilterComboBox.setOnAction(e -> filterAuctions(auctionSearchField.getText()));
  }

  private void autofillForm(AuctionPreview summary) {
    Item item = ItemStore.getInstance().getItem(summary.itemId());
    nameField.setText(summary.itemName());
    descriptionField.setText(item != null ? item.getDescription() : "");
    typeComboBox.setValue((item != null && item.getType() != null) ? item.getType() : ItemType.ART);

    startingPriceField.setText(String.valueOf(item != null ? item.getStartingPrice() : 0));
    stepPriceField.setText(String.valueOf(item != null ? item.getStepPrice() : 0));

    if (summary.startTime() != null) {
      startDatePicker.setValue(summary.startTime().toLocalDate());
      startTimeField.setText(summary.startTime().toLocalTime().format(timeFormatter));
    } else {
      startDatePicker.setValue(LocalDate.now());
      startTimeField.setText("12:00");
    }
    if (endDatePicker != null && endTimeField != null && summary.endTime() != null) {
      endDatePicker.setValue(summary.endTime().toLocalDate());
      endTimeField.setText(summary.endTime().toLocalTime().format(timeFormatter));
    } else if (endDatePicker != null && endTimeField != null) {
      LocalDateTime defaultEnd = LocalDateTime.of(startDatePicker.getValue(), LocalTime.of(13, 0));
      endDatePicker.setValue(defaultEnd.toLocalDate());
      endTimeField.setText(defaultEnd.toLocalTime().format(timeFormatter));
    }
    if (durationField != null && summary.startTime() != null && summary.endTime() != null) {
      long durationMins =
          java.time.Duration.between(summary.startTime(), summary.endTime()).toMinutes();
      durationField.setText(String.valueOf(durationMins));
    }
  }

  // Master Data Loaders
  private void loadAuctionsData() {
    masterAuctions.clear();
    masterAuctions.addAll(AuctionStore.getInstance().getAuctionPreviews());

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

  private void loadUsersData() {
    filterUsers(userSearchField.getText());
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
                  Item item = ItemStore.getInstance().getItem(a.itemId());
                  return item != null
                      && item.getType() != null
                      && item.getType().name().equals(type);
                })
            .toList();
    auctionTableView.getItems().setAll(filtered);
  }

  private void filterUsers(String query) {
    List<UserDto> users = UserListStore.getInstance().getMasterUsers();
    if (query == null || query.isBlank()) {
      userTableView.getItems().setAll(users);
      return;
    }
    String keyword = query.toLowerCase().trim();
    List<UserDto> filtered =
        users.stream()
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

  private void handleToggleUserBan(UserDto selected, ActionEvent event) {
    if (actionLoading) return;
    if (!requests.isConnected()) {
      AlertUtils.showError("Mất kết nối", "Mất kết nối tới máy chủ.");
      return;
    }
    if (selected == null) {
      AlertUtils.showError("Chưa chọn tài khoản", "Vui lòng chọn tài khoản cần xử lý.");
      return;
    }
    if (isCurrentUser(selected)) {
      AlertUtils.showError("Không hợp lệ", "Không thể tự cấm chính mình.");
      return;
    }

    boolean ban = !selected.isBanned();
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle(ban ? "Xác nhận cấm tài khoản" : "Xác nhận mở khóa tài khoản");
    alert.setHeaderText(
        (ban ? "Cấm tài khoản " : "Mở khóa tài khoản ") + selected.account().username() + "?");
    alert.setContentText(
        ban
            ? "Tài khoản bị cấm sẽ không thể đăng nhập hoặc gửi yêu cầu lên server."
            : "Tài khoản này sẽ được phép đăng nhập và sử dụng hệ thống trở lại.");

    if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
      return;
    }

    try {
      currentLoadingButton = LoadingButton.fromEvent(event);
      pendingAction = PendingAction.USER;
      setActionLoading(true);
      requests.banUser(selected.id(), ban);
    } catch (IOException e) {
      setActionLoading(false);
      AlertUtils.showError("Lỗi kết nối", "Server không phản hồi.");
    }
  }

  private boolean isCurrentUser(UserDto user) {
    var currentUser = UserManager.getInstance().getCurrentUser();
    return user != null && currentUser != null && user.id() == currentUser.getId();
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
    startingPriceField.clear();
    stepPriceField.clear();
    startDatePicker.setValue(LocalDate.now());
    startTimeField.setText("12:00");
    if (endDatePicker != null) {
      endDatePicker.setValue(LocalDate.now());
    }
    if (endTimeField != null) {
      endTimeField.setText("13:00");
    }
    if (durationField != null) {
      durationField.setText("60");
    }
    auctionTableView.getSelectionModel().clearSelection();
  }

  // CRUD actions
  @FXML
  public void handleCreateAuction(ActionEvent event) {
    AlertUtils.showError("Không được phép", "Admin không được tạo phiên đấu giá mới.");
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
      pendingAction = PendingAction.AUCTION;
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
        pendingAction = PendingAction.AUCTION;
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
    ItemType type = typeComboBox.getValue();
    LocalDate startDate = startDatePicker.getValue();
    String timeStr = startTimeField.getText().trim();
    LocalDate endDate = endDatePicker == null ? null : endDatePicker.getValue();
    String endTimeStr = fieldText(endTimeField);
    String durationStr = fieldText(durationField);
    boolean hasEndTime = endDate != null && !endTimeStr.isEmpty();

    if (name.isEmpty()
        || startingStr.isEmpty()
        || stepStr.isEmpty()
        || startDate == null
        || timeStr.isEmpty()
        || (!hasEndTime && durationStr.isEmpty())) {
      AlertUtils.showError("Thiếu thông tin", "Vui lòng nhập đầy đủ các thông tin bắt buộc (*).");
      return null;
    }

    try {
      long startingPrice = Long.parseLong(startingStr);
      long stepPrice = Long.parseLong(stepStr);
      LocalTime startTime = LocalTime.parse(timeStr, timeFormatter);
      LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);
      int durationMins =
          hasEndTime
              ? durationBetween(startDateTime, endDate, endTimeStr)
              : Integer.parseInt(durationStr);

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
          "Sai định dạng số", "Giá khởi điểm và bước giá phải là số nguyên hợp lệ.");
      return null;
    } catch (DateTimeParseException e) {
      AlertUtils.showError(
          "Sai định dạng giờ",
          "Giờ bắt đầu và kết thúc phải tuân theo định dạng HH:mm (ví dụ: 14:30).");
      return null;
    } catch (IllegalArgumentException e) {
      AlertUtils.showError("Dữ liệu không hợp lệ", e.getMessage());
      return null;
    }
  }

  private int durationBetween(LocalDateTime startDateTime, LocalDate endDate, String endTimeStr) {
    LocalTime endTime = LocalTime.parse(endTimeStr, timeFormatter);
    LocalDateTime endDateTime = LocalDateTime.of(endDate, endTime);
    if (!endDateTime.isAfter(startDateTime)) {
      throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu.");
    }
    long durationMins = java.time.Duration.between(startDateTime, endDateTime).toMinutes();
    if (durationMins <= 0 || durationMins > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Thời gian diễn ra không hợp lệ.");
    }
    return (int) durationMins;
  }

  private String fieldText(TextField field) {
    return field == null || field.getText() == null ? "" : field.getText().trim();
  }

  // Handle Response Message
  private void handleIncomingMessage(String msg) {
    if (!actionLoading) return;
    PendingAction completedAction = pendingAction;
    setActionLoading(false);

    if (msg != null
        && (msg.toLowerCase().contains("thành công") || msg.toLowerCase().contains("ok"))) {
      AlertUtils.showInfo("Thành công", msg);
      if (completedAction == PendingAction.USER) {
        refreshUsers();
      } else {
        handleClearForm();
        refreshAuctions();
      }
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
      pendingAction = PendingAction.NONE;
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
