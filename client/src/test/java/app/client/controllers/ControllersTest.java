package app.client.controllers;

import static org.junit.jupiter.api.Assertions.*;

import app.client.Client;
import app.client.manager.NavigationManager;
import app.client.manager.UserManager;
import app.client.store.AuctionStore;
import app.client.store.LiveAuctionSessionStore;
import app.client.store.UserListStore;
import app.common.dto.*;
import app.common.enums.AuctionStatus;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
import app.common.models.*;
import java.io.BufferedWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ControllersTest {

  private Thread closerThread;

  @BeforeEach
  void setUp() throws Exception {
    try {
      Platform.startup(() -> {});
      Platform.setImplicitExit(false);
    } catch (IllegalStateException e) {
      // Already initialized
    }

    // Set up a mock Stage on FX thread for NavigationManager
    final Stage[] stageHolder = new Stage[1];
    java.util.concurrent.CountDownLatch stageLatch = new java.util.concurrent.CountDownLatch(1);
    Platform.runLater(
        () -> {
          stageHolder[0] = new Stage();
          stageLatch.countDown();
        });
    stageLatch.await();
    NavigationManager.getInstance().setPrimaryStage(stageHolder[0]);

    // Mock client status via reflection to prevent ConnectException
    Client clientInstance = Client.getInstance();
    setPrivateField(clientInstance, "connected", true);
    setPrivateField(clientInstance, "closed", false);
    setPrivateField(clientInstance, "writer", new BufferedWriter(new StringWriter()));

    UserManager.getInstance().setCurrentUser(null);
    AuctionStore.getInstance().clearHistory();
    UserListStore.getInstance().clear();

    // Start a daemon closer thread to auto-dismiss alert dialogs in headless runs
    closerThread =
        new Thread(
            () -> {
              try {
                while (!Thread.currentThread().isInterrupted()) {
                  Thread.sleep(50);
                  Platform.runLater(
                      () -> {
                        for (Window window : new ArrayList<>(Window.getWindows())) {
                          if (window.isShowing()) {
                            window.hide();
                          }
                        }
                      });
                }
              } catch (InterruptedException e) {
                // Terminated
              }
            });
    closerThread.setDaemon(true);
    closerThread.start();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (closerThread != null) {
      closerThread.interrupt();
    }
    // Restore singleton states to prevent test pollution
    Client clientInstance = Client.getInstance();
    setPrivateField(clientInstance, "connected", false);
    setPrivateField(clientInstance, "closed", true);
    setPrivateField(clientInstance, "writer", null);

    UserManager.getInstance().setCurrentUser(null);
    AuctionStore.getInstance().clearHistory();
    LiveAuctionSessionStore.getInstance().clear();
    UserListStore.getInstance().clear();
  }

  private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private Object invokePrivateMethod(
      Object target, String methodName, Class<?>[] parameterTypes, Object[] args) throws Exception {
    Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
    method.setAccessible(true);
    return method.invoke(target, args);
  }

  private void runOnFxThread(Runnable runnable) throws Exception {
    java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
    final Throwable[] throwableHolder = new Throwable[1];
    Platform.runLater(
        () -> {
          try {
            runnable.run();
          } catch (Throwable t) {
            throwableHolder[0] = t;
          } finally {
            latch.countDown();
          }
        });
    latch.await();
    if (throwableHolder[0] != null) {
      if (throwableHolder[0] instanceof Exception) {
        throw (Exception) throwableHolder[0];
      } else {
        throw new RuntimeException(throwableHolder[0]);
      }
    }
  }

  @Test
  public void testConnectServerController() throws Exception {
    runOnFxThread(
        () -> {
          try {
            ConnectServerController controller = new ConnectServerController();

            Button connectBtn = new Button("Connect");
            Label statusLbl = new Label("Status");
            AnchorPane pane = new AnchorPane();

            setPrivateField(controller, "connectButton", connectBtn);
            setPrivateField(controller, "statusLabel", statusLbl);
            setPrivateField(controller, "rootPane", pane);

            assertDoesNotThrow(() -> controller.connectServer(null));
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  public void testLoginController() throws Exception {
    runOnFxThread(
        () -> {
          try {
            LoginController controller = new LoginController();

            TextField accountField = new TextField("john_doe");
            PasswordField passwordField = new PasswordField();
            passwordField.setText("mypass");
            Button loginBtn = new Button("Login");
            Label lblReg = new Label("Register");
            AnchorPane pane = new AnchorPane();

            setPrivateField(controller, "account", accountField);
            setPrivateField(controller, "password", passwordField);
            setPrivateField(controller, "loginButton", loginBtn);
            setPrivateField(controller, "lblRegister", lblReg);
            setPrivateField(controller, "rootPane", pane);

            assertDoesNotThrow(() -> controller.handleMouseEntered(null));
            assertTrue(lblReg.isUnderline());

            assertDoesNotThrow(() -> controller.handleMouseExited(null));
            assertFalse(lblReg.isUnderline());

            assertDoesNotThrow(() -> controller.switchToRegister(null));
            assertDoesNotThrow(controller::switchToUi);

            // Call handleLogin with non-empty inputs
            assertDoesNotThrow(controller::handleLogin);

            // Test handleLoginResult via reflection
            User mockUserAdmin =
                new User(
                    1,
                    "Admin User",
                    new Account("admin", "pass", UserRole.ADMIN),
                    new Wallet(BigDecimal.TEN));
            UserManager.getInstance().setCurrentUser(mockUserAdmin);
            setPrivateField(controller, "loginLoading", true);
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "handleLoginResult",
                        new Class<?>[] {String.class},
                        new Object[] {"OK"}));

            User mockUserBidder =
                new User(
                    2,
                    "Bidder User",
                    new Account("bidder", "pass", UserRole.BIDDER),
                    new Wallet(BigDecimal.TEN));
            UserManager.getInstance().setCurrentUser(mockUserBidder);
            setPrivateField(controller, "loginLoading", true);
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "handleLoginResult",
                        new Class<?>[] {String.class},
                        new Object[] {"OK"}));

            // Cover private setLoginLoading variations
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "setLoginLoading",
                        new Class<?>[] {boolean.class},
                        new Object[] {true}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "setLoginLoading",
                        new Class<?>[] {boolean.class},
                        new Object[] {false}));

            assertDoesNotThrow(controller::cleanup);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  public void testRegisterController() throws Exception {
    runOnFxThread(
        () -> {
          try {
            RegisterController controller = new RegisterController();

            AnchorPane root = new AnchorPane();
            Label lblLogin = new Label("Login");
            TextField name = new TextField("Test User");
            TextField account = new TextField("test_acc");
            PasswordField pass = new PasswordField();
            pass.setText("password");
            RadioButton rbSeller = new RadioButton();
            RadioButton rbBidder = new RadioButton();
            Button regButton = new Button("Register");

            setPrivateField(controller, "rootPane", root);
            setPrivateField(controller, "lblLogin", lblLogin);
            setPrivateField(controller, "txtName", name);
            setPrivateField(controller, "txtAccount", account);
            setPrivateField(controller, "txtPassword", pass);
            setPrivateField(controller, "rbSeller", rbSeller);
            setPrivateField(controller, "rbBidder", rbBidder);
            setPrivateField(controller, "registerButton", regButton);

            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(controller, "initialize", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(() -> controller.handleRegister(new ActionEvent()));

            rbSeller.setSelected(true);
            assertDoesNotThrow(() -> controller.handleRegister(new ActionEvent()));

            setPrivateField(controller, "registerLoading", true);
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "handleRegisterResult",
                        new Class<?>[] {String.class},
                        new Object[] {"OK"}));

            // Cover private setRegisterLoading variations
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "setRegisterLoading",
                        new Class<?>[] {boolean.class},
                        new Object[] {true}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "setRegisterLoading",
                        new Class<?>[] {boolean.class},
                        new Object[] {false}));

            assertDoesNotThrow(() -> controller.backToLoginMouse(null));
            assertDoesNotThrow(controller::cleanup);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  public void testDepositController() throws Exception {
    runOnFxThread(
        () -> {
          try {
            DepositController controller = new DepositController();

            TextField amountField = new TextField("1500");
            Label totalLbl = new Label("Total");
            Label availableLbl = new Label("Available");

            setPrivateField(controller, "depositAmountField", amountField);
            setPrivateField(controller, "totalBalanceLabel", totalLbl);
            setPrivateField(controller, "availableBalanceLabel", availableLbl);

            User mockUser =
                new User(
                    1,
                    "Test User",
                    new Account("test", "pass", UserRole.BIDDER),
                    new Wallet(BigDecimal.TEN));
            UserManager.getInstance().setCurrentUser(mockUser);

            assertDoesNotThrow(controller::initialize);
            assertDoesNotThrow(() -> controller.handleDeposit(new ActionEvent()));

            // Test handleWalletUpdate via reflection
            setPrivateField(controller, "depositLoading", true);
            setPrivateField(controller, "balanceBeforeDeposit", BigDecimal.ZERO);
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "handleWalletUpdate",
                        new Class<?>[] {String.class},
                        new Object[] {"OK"}));

            // Cover private helper methods
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "currentTotalBalance", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "hasBalanceChanged", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "isSuccessMessage",
                        new Class<?>[] {String.class},
                        new Object[] {"thành công"}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "setDepositLoading",
                        new Class<?>[] {boolean.class},
                        new Object[] {true}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "setDepositLoading",
                        new Class<?>[] {boolean.class},
                        new Object[] {false}));

            assertDoesNotThrow(() -> controller.handleBack(new ActionEvent()));
            assertDoesNotThrow(controller::cleanup);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  public void testMessController() throws Exception {
    runOnFxThread(
        () -> {
          try {
            MessController controller = new MessController();

            TextArea myTextArea = new TextArea("Hello, World!");
            VBox chatBox = new VBox();
            ScrollPane scrollPane = new ScrollPane();

            setPrivateField(controller, "myTextArea", myTextArea);
            setPrivateField(controller, "chatBox", chatBox);
            setPrivateField(controller, "scrollPane", scrollPane);

            User mockUser =
                new User(
                    1,
                    "Test User",
                    new Account("test", "pass", UserRole.BIDDER),
                    new Wallet(BigDecimal.TEN));
            UserManager.getInstance().setCurrentUser(mockUser);

            assertDoesNotThrow(controller::initialize);
            assertTrue(controller.isMe(1));
            assertFalse(controller.isMe(2));

            assertDoesNotThrow(() -> controller.addBubble("Other User", "Hello @test", false));
            assertDoesNotThrow(() -> controller.addBubble("Test User", "My msg", true));

            assertDoesNotThrow(controller::send);

            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "handleMessage",
                        new Class<?>[] {String.class},
                        new Object[] {"OK"}));
            assertDoesNotThrow(() -> controller.switchToUi(new ActionEvent()));
            assertDoesNotThrow(controller::cleanup);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  public void testUserProfileController() throws Exception {
    runOnFxThread(
        () -> {
          try {
            UserProfileController controller = new UserProfileController();

            AnchorPane root = new AnchorPane();
            Circle avatarCircle = new Circle();
            Label avatarLabel = new Label("T");
            ImageView avatarImageView = new ImageView();
            Label userNameLabel = new Label();
            Label emailLabel = new Label();
            Label walletLabel = new Label();
            Label roleLabel = new Label();
            Label idLabel = new Label();

            setPrivateField(controller, "rootPane", root);
            setPrivateField(controller, "avatarCircle", avatarCircle);
            setPrivateField(controller, "avatarLabel", avatarLabel);
            setPrivateField(controller, "avatarImageView", avatarImageView);
            setPrivateField(controller, "userNameLabel", userNameLabel);
            setPrivateField(controller, "emailLabel", emailLabel);
            setPrivateField(controller, "walletLabel", walletLabel);
            setPrivateField(controller, "roleLabel", roleLabel);
            setPrivateField(controller, "idLabel", idLabel);

            User mockUser =
                new User(
                    1,
                    "Test User",
                    new Account("test", "pass", UserRole.BIDDER),
                    new Wallet(BigDecimal.TEN));
            mockUser.setAvatarUrl("avatar.png");
            UserManager.getInstance().setCurrentUser(mockUser);
            UserManager.getInstance()
                .setAvatarBase64(
                    1,
                    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=");

            assertDoesNotThrow(controller::initialize);
            assertDoesNotThrow(() -> controller.handleLogout(new ActionEvent()));
            assertDoesNotThrow(() -> controller.handleBack(new ActionEvent()));
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  public void testAuctionController() throws Exception {
    runOnFxThread(
        () -> {
          try {
            AuctionController controller = new AuctionController();

            TextField nameField = new TextField("Auction Item");
            TextArea descriptionField = new TextArea("Some Description");
            TextField startingPriceField = new TextField("1000");
            TextField stepPriceField = new TextField("100");
            ComboBox<ItemType> typeComboBox = new ComboBox<>();
            TextField durationField = new TextField("60");
            DatePicker startDatePicker = new DatePicker(LocalDate.now());
            TextField startTimeField = new TextField("14:30");
            Button chooseImageButton = new Button("Choose");
            Label imageFileNameLabel = new Label();

            setPrivateField(controller, "nameField", nameField);
            setPrivateField(controller, "descriptionField", descriptionField);
            setPrivateField(controller, "startingPriceField", startingPriceField);
            setPrivateField(controller, "stepPriceField", stepPriceField);
            setPrivateField(controller, "typeComboBox", typeComboBox);
            setPrivateField(controller, "durationField", durationField);
            setPrivateField(controller, "startDatePicker", startDatePicker);
            setPrivateField(controller, "startTimeField", startTimeField);
            setPrivateField(controller, "chooseImageButton", chooseImageButton);
            setPrivateField(controller, "imageFileNameLabel", imageFileNameLabel);

            User mockUser =
                new User(
                    1,
                    "Test User",
                    new Account("test", "pass", UserRole.SELLER),
                    new Wallet(BigDecimal.TEN));
            UserManager.getInstance().setCurrentUser(mockUser);

            assertDoesNotThrow(controller::initialize);
            assertDoesNotThrow(() -> controller.handleAdd(new ActionEvent()));

            setPrivateField(controller, "createLoading", true);
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "handleCreateAuctionResult",
                        new Class<?>[] {String.class},
                        new Object[] {"OK"}));

            // Cover private helper methods
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "isSuccessMessage",
                        new Class<?>[] {String.class},
                        new Object[] {"thành công"}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "setCreateLoading",
                        new Class<?>[] {boolean.class},
                        new Object[] {true}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "setCreateLoading",
                        new Class<?>[] {boolean.class},
                        new Object[] {false}));

            assertDoesNotThrow(() -> controller.handleBack(new ActionEvent()));
            assertDoesNotThrow(controller::cleanup);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  public void testMyHistoryController() throws Exception {
    runOnFxThread(
        () -> {
          try {
            MyHistoryController controller = new MyHistoryController();

            FlowPane runningPane = new FlowPane();
            ComboBox<String> typeFilterComboBox = new ComboBox<>();
            FlowPane finishedPane = new FlowPane();

            setPrivateField(controller, "runningPane", runningPane);
            setPrivateField(controller, "typeFilterComboBox", typeFilterComboBox);
            setPrivateField(controller, "finishedPane", finishedPane);

            User mockUser =
                new User(
                    1,
                    "Test User",
                    new Account("test", "pass", UserRole.BIDDER),
                    new Wallet(BigDecimal.TEN));
            UserManager.getInstance().setCurrentUser(mockUser);

            AuctionStore.getInstance().setHistoryAuctions(new ArrayList<>());
            assertDoesNotThrow(controller::initialize);

            assertDoesNotThrow(() -> controller.handleReload(new ActionEvent()));

            // Cover private helper methods
            AuctionPreview preview =
                new AuctionPreview(
                    1,
                    1,
                    "Name",
                    "img.png",
                    ItemType.ART,
                    AuctionStatus.RUNNING,
                    LocalDateTime.now().minusDays(1),
                    LocalDateTime.now().plusDays(1),
                    1000,
                    1000,
                    100,
                    1,
                    new UserPreview(5, "Seller", "username", UserRole.SELLER, "avatar.png"));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "requestHistoryIfStale",
                        new Class<?>[] {boolean.class},
                        new Object[] {true}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "requestHistoryIfStale",
                        new Class<?>[] {boolean.class},
                        new Object[] {false}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "loadCachedHistory", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () -> invokePrivateMethod(controller, "rebuildUi", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "isActiveStatus",
                        new Class<?>[] {AuctionStatus.class},
                        new Object[] {AuctionStatus.RUNNING}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "timeText",
                        new Class<?>[] {AuctionPreview.class},
                        new Object[] {preview}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "countdownText",
                        new Class<?>[] {LocalDateTime.class},
                        new Object[] {LocalDateTime.now().plusHours(1)}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "itemName",
                        new Class<?>[] {AuctionPreview.class},
                        new Object[] {preview}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "setReloadLoading",
                        new Class<?>[] {boolean.class},
                        new Object[] {true}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "setReloadLoading",
                        new Class<?>[] {boolean.class},
                        new Object[] {false}));

            assertDoesNotThrow(controller::switchToUi);
            assertDoesNotThrow(controller::cleanup);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  public void testAllAuctionController() throws Exception {
    runOnFxThread(
        () -> {
          try {
            AllAuctionController controller = new AllAuctionController();

            FlowPane runningPane = new FlowPane();
            ComboBox<String> typeFilterComboBox = new ComboBox<>();
            TextField searchField = new TextField();
            FlowPane finishedPane = new FlowPane();

            setPrivateField(controller, "runningPane", runningPane);
            setPrivateField(controller, "typeFilterComboBox", typeFilterComboBox);
            setPrivateField(controller, "searchField", searchField);
            setPrivateField(controller, "finishedPane", finishedPane);

            assertDoesNotThrow(controller::initialize);
            assertDoesNotThrow(() -> controller.handleReload(new ActionEvent()));

            // Cover private helper methods
            AuctionPreview preview =
                new AuctionPreview(
                    1,
                    1,
                    "Name",
                    "img.png",
                    ItemType.ART,
                    AuctionStatus.RUNNING,
                    LocalDateTime.now().minusDays(1),
                    LocalDateTime.now().plusDays(1),
                    1000,
                    1000,
                    100,
                    1,
                    new UserPreview(5, "Seller", "username", UserRole.SELLER, "avatar.png"));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "requestAuctions", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () -> invokePrivateMethod(controller, "rebuildUi", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "isActiveStatus",
                        new Class<?>[] {AuctionStatus.class},
                        new Object[] {AuctionStatus.RUNNING}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "timeText",
                        new Class<?>[] {AuctionPreview.class},
                        new Object[] {preview}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "countdownText",
                        new Class<?>[] {LocalDateTime.class},
                        new Object[] {LocalDateTime.now().plusHours(1)}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "itemName",
                        new Class<?>[] {AuctionPreview.class},
                        new Object[] {preview}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "setReloadLoading",
                        new Class<?>[] {boolean.class},
                        new Object[] {true}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "setReloadLoading",
                        new Class<?>[] {boolean.class},
                        new Object[] {false}));

            assertDoesNotThrow(controller::switchToUi);
            assertDoesNotThrow(controller::cleanup);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  public void testFirstScene() throws Exception {
    runOnFxThread(
        () -> {
          try {
            FirstScene controller = new FirstScene();

            TextField searchField = new TextField();
            ListView<AuctionPreview> auctionListView = new ListView<>();
            Button btnAuth = new Button();
            StackPane activeAuctionsPane = new StackPane();
            StackPane completedAuctionsPane = new StackPane();
            StackPane upcomingAuctionsPane = new StackPane();
            ScrollPane contentScrollPane = new ScrollPane();
            Label balanceLabel = new Label();

            setPrivateField(controller, "searchField", searchField);
            setPrivateField(controller, "auctionListView", auctionListView);
            setPrivateField(controller, "btnAuth", btnAuth);
            setPrivateField(controller, "activeAuctionsPane", activeAuctionsPane);
            setPrivateField(controller, "completedAuctionsPane", completedAuctionsPane);
            setPrivateField(controller, "upcomingAuctionsPane", upcomingAuctionsPane);
            setPrivateField(controller, "contentScrollPane", contentScrollPane);
            setPrivateField(controller, "balanceLabel", balanceLabel);

            User mockUser =
                new User(
                    1,
                    "Test User",
                    new Account("test", "pass", UserRole.BIDDER),
                    new Wallet(BigDecimal.TEN));
            UserManager.getInstance().setCurrentUser(mockUser);

            // Seed mock auctions in store
            AuctionPreview preview =
                new AuctionPreview(
                    1,
                    1,
                    "Item A",
                    "img.png",
                    ItemType.ART,
                    AuctionStatus.OPEN,
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now().plusDays(2),
                    1000,
                    1000,
                    100,
                    1,
                    new UserPreview(5, "SellerName", "seller_user", UserRole.SELLER, "avatar.png"));
            AuctionStore.getInstance().addPreview(preview);
            AuctionStore.getInstance()
                .addPreview(
                    new AuctionPreview(
                        2,
                        2,
                        "Item B",
                        "img.png",
                        ItemType.VEHICLE,
                        AuctionStatus.RUNNING,
                        LocalDateTime.now().minusDays(1),
                        LocalDateTime.now().plusDays(1),
                        2000,
                        1500,
                        100,
                        1,
                        new UserPreview(
                            5, "SellerName", "seller_user", UserRole.SELLER, "avatar.png")));
            AuctionStore.getInstance()
                .addPreview(
                    new AuctionPreview(
                        3,
                        3,
                        "Item C",
                        "img.png",
                        ItemType.ELECTRONICS,
                        AuctionStatus.FINISHED,
                        LocalDateTime.now().minusDays(2),
                        LocalDateTime.now().minusDays(1),
                        3000,
                        2000,
                        100,
                        1,
                        new UserPreview(
                            5, "SellerName", "seller_user", UserRole.SELLER, "avatar.png")));

            assertDoesNotThrow(controller::initialize);
            assertDoesNotThrow(() -> controller.handleReload(new ActionEvent()));
            assertDoesNotThrow(() -> controller.handleAuth(new ActionEvent()));
            assertDoesNotThrow(() -> controller.switchToLive(new ActionEvent()));
            assertDoesNotThrow(() -> controller.switchToMine(new ActionEvent()));
            assertDoesNotThrow(() -> controller.switchToMess(new ActionEvent()));
            assertDoesNotThrow(() -> controller.switchToOrganize(new ActionEvent()));
            assertDoesNotThrow(() -> controller.switchToDeposit(new ActionEvent()));
            assertDoesNotThrow(() -> controller.switchToAll(new ActionEvent()));

            // Test switchMine with Seller role
            mockUser.getAccount().setRole(UserRole.SELLER);
            assertDoesNotThrow(() -> controller.switchToMine(new ActionEvent()));

            // Cover private helper methods
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "setupListView", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "setupAuthButton", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(controller, "setupSearch", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "setupScrollPanes", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "loadInitialData", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () -> invokePrivateMethod(controller, "rebuildUi", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "updateListView", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "priceText",
                        new Class<?>[] {AuctionPreview.class},
                        new Object[] {preview}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "timeText",
                        new Class<?>[] {AuctionPreview.class},
                        new Object[] {preview}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "detailButtonText",
                        new Class<?>[] {AuctionStatus.class},
                        new Object[] {AuctionStatus.OPEN}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "itemName",
                        new Class<?>[] {AuctionPreview.class},
                        new Object[] {preview}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "setupWalletSection", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "updateBalanceLabel", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "setReloadLoading",
                        new Class<?>[] {boolean.class},
                        new Object[] {true}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "setReloadLoading",
                        new Class<?>[] {boolean.class},
                        new Object[] {false}));

            assertDoesNotThrow(controller::cleanup);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  public void testSellerDashboardController() throws Exception {
    runOnFxThread(
        () -> {
          try {
            SellerDashboardController controller = new SellerDashboardController();

            Label sellerNameLabel = new Label();
            TextField auctionSearchField = new TextField();
            ComboBox<String> typeFilterComboBox = new ComboBox<>();
            TableView<AuctionPreview> auctionTableView = new TableView<>();
            TableColumn<AuctionPreview, Integer> colAuctionId = new TableColumn<>();
            TableColumn<AuctionPreview, String> colItemName = new TableColumn<>();
            TableColumn<AuctionPreview, String> colItemType = new TableColumn<>();
            TableColumn<AuctionPreview, String> colStartingPrice = new TableColumn<>();
            TableColumn<AuctionPreview, String> colHighestBid = new TableColumn<>();
            TableColumn<AuctionPreview, String> colStartTime = new TableColumn<>();
            TableColumn<AuctionPreview, String> colStatus = new TableColumn<>();

            TextField nameField = new TextField("Item Name");
            TextArea descriptionField = new TextArea("Desc");
            ComboBox<ItemType> typeComboBox = new ComboBox<>();
            TextField durationField = new TextField("60");
            TextField startingPriceField = new TextField("1000");
            TextField stepPriceField = new TextField("100");
            DatePicker startDatePicker = new DatePicker(LocalDate.now());
            TextField startTimeField = new TextField("15:00");

            setPrivateField(controller, "sellerNameLabel", sellerNameLabel);
            setPrivateField(controller, "auctionSearchField", auctionSearchField);
            setPrivateField(controller, "typeFilterComboBox", typeFilterComboBox);
            setPrivateField(controller, "auctionTableView", auctionTableView);
            setPrivateField(controller, "colAuctionId", colAuctionId);
            setPrivateField(controller, "colItemName", colItemName);
            setPrivateField(controller, "colItemType", colItemType);
            setPrivateField(controller, "colStartingPrice", colStartingPrice);
            setPrivateField(controller, "colHighestBid", colHighestBid);
            setPrivateField(controller, "colStartTime", colStartTime);
            setPrivateField(controller, "colStatus", colStatus);
            setPrivateField(controller, "nameField", nameField);
            setPrivateField(controller, "descriptionField", descriptionField);
            setPrivateField(controller, "typeComboBox", typeComboBox);
            setPrivateField(controller, "durationField", durationField);
            setPrivateField(controller, "startingPriceField", startingPriceField);
            setPrivateField(controller, "stepPriceField", stepPriceField);
            setPrivateField(controller, "startDatePicker", startDatePicker);
            setPrivateField(controller, "startTimeField", startTimeField);

            User mockUser =
                new User(
                    5,
                    "Test Seller",
                    new Account("test", "pass", UserRole.SELLER),
                    new Wallet(BigDecimal.TEN));
            UserManager.getInstance().setCurrentUser(mockUser);

            AuctionPreview preview =
                new AuctionPreview(
                    1,
                    1,
                    "Item Name",
                    "img.png",
                    ItemType.ART,
                    AuctionStatus.OPEN,
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now().plusDays(2),
                    1000,
                    1000,
                    100,
                    1,
                    new UserPreview(5, "SellerName", "seller_user", UserRole.SELLER, "avatar.png"));
            AuctionStore.getInstance().addPreview(preview);

            assertDoesNotThrow(controller::initialize);
            assertDoesNotThrow(() -> controller.handleRefreshAuctions(new ActionEvent()));
            assertDoesNotThrow(() -> controller.handleBackToMain(new ActionEvent()));
            assertDoesNotThrow(controller::handleClearForm);

            assertDoesNotThrow(() -> controller.handleCreateAuction(new ActionEvent()));
            auctionTableView.getSelectionModel().select(preview);
            assertDoesNotThrow(() -> controller.handleUpdateAuction(new ActionEvent()));

            setPrivateField(controller, "actionLoading", true);
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "handleIncomingMessage",
                        new Class<?>[] {String.class},
                        new Object[] {"OK"}));

            // Cover private helper methods
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "setupGreeter", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "setupAuctionsTable", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () -> invokePrivateMethod(controller, "setupForm", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "setupSearchFilters", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "autofillForm",
                        new Class<?>[] {AuctionPreview.class},
                        new Object[] {preview}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "loadAuctionsData", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "getStatusScore",
                        new Class<?>[] {AuctionStatus.class},
                        new Object[] {AuctionStatus.RUNNING}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "filterAuctions",
                        new Class<?>[] {String.class},
                        new Object[] {"test"}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "setActionLoading",
                        new Class<?>[] {boolean.class},
                        new Object[] {true}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "setActionLoading",
                        new Class<?>[] {boolean.class},
                        new Object[] {false}));

            assertDoesNotThrow(controller::cleanup);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  public void testAdminDashboardController() throws Exception {
    runOnFxThread(
        () -> {
          try {
            AdminDashboardController controller = new AdminDashboardController();

            Label adminNameLabel = new Label();
            TextField auctionSearchField = new TextField();
            TextField userSearchField = new TextField();
            ComboBox<String> typeFilterComboBox = new ComboBox<>();
            TableView<AuctionPreview> auctionTableView = new TableView<>();
            TableColumn<AuctionPreview, Integer> colAuctionId = new TableColumn<>();
            TableColumn<AuctionPreview, String> colItemName = new TableColumn<>();
            TableColumn<AuctionPreview, String> colItemType = new TableColumn<>();
            TableColumn<AuctionPreview, String> colStartingPrice = new TableColumn<>();
            TableColumn<AuctionPreview, String> colHighestBid = new TableColumn<>();
            TableColumn<AuctionPreview, String> colStartTime = new TableColumn<>();
            TableColumn<AuctionPreview, String> colStatus = new TableColumn<>();

            TextField nameField = new TextField("Item Name");
            TextArea descriptionField = new TextArea("Desc");
            ComboBox<ItemType> typeComboBox = new ComboBox<>();
            TextField durationField = new TextField("60");
            TextField startingPriceField = new TextField("1000");
            TextField stepPriceField = new TextField("100");
            DatePicker startDatePicker = new DatePicker(LocalDate.now());
            TextField startTimeField = new TextField("15:00");

            TableView<UserDto> userTableView = new TableView<>();
            TableColumn<UserDto, Integer> colUserId = new TableColumn<>();
            TableColumn<UserDto, String> colUserName = new TableColumn<>();
            TableColumn<UserDto, String> colUserAccount = new TableColumn<>();
            TableColumn<UserDto, String> colUserBalance = new TableColumn<>();
            TableColumn<UserDto, String> colUserRole = new TableColumn<>();
            TableColumn<UserDto, String> colUserStatus = new TableColumn<>();
            TableColumn<UserDto, Void> colUserAction = new TableColumn<>();

            setPrivateField(controller, "adminNameLabel", adminNameLabel);
            setPrivateField(controller, "auctionSearchField", auctionSearchField);
            setPrivateField(controller, "userSearchField", userSearchField);
            setPrivateField(controller, "typeFilterComboBox", typeFilterComboBox);
            setPrivateField(controller, "auctionTableView", auctionTableView);
            setPrivateField(controller, "colAuctionId", colAuctionId);
            setPrivateField(controller, "colItemName", colItemName);
            setPrivateField(controller, "colItemType", colItemType);
            setPrivateField(controller, "colStartingPrice", colStartingPrice);
            setPrivateField(controller, "colHighestBid", colHighestBid);
            setPrivateField(controller, "colStartTime", colStartTime);
            setPrivateField(controller, "colStatus", colStatus);
            setPrivateField(controller, "nameField", nameField);
            setPrivateField(controller, "descriptionField", descriptionField);
            setPrivateField(controller, "typeComboBox", typeComboBox);
            setPrivateField(controller, "durationField", durationField);
            setPrivateField(controller, "startingPriceField", startingPriceField);
            setPrivateField(controller, "stepPriceField", stepPriceField);
            setPrivateField(controller, "startDatePicker", startDatePicker);
            setPrivateField(controller, "startTimeField", startTimeField);
            setPrivateField(controller, "userTableView", userTableView);
            setPrivateField(controller, "colUserId", colUserId);
            setPrivateField(controller, "colUserName", colUserName);
            setPrivateField(controller, "colUserAccount", colUserAccount);
            setPrivateField(controller, "colUserBalance", colUserBalance);
            setPrivateField(controller, "colUserRole", colUserRole);
            setPrivateField(controller, "colUserStatus", colUserStatus);
            setPrivateField(controller, "colUserAction", colUserAction);

            User mockUser =
                new User(
                    1,
                    "Test Admin",
                    new Account("test", "pass", UserRole.ADMIN),
                    new Wallet(BigDecimal.TEN));
            UserManager.getInstance().setCurrentUser(mockUser);

            AuctionPreview preview =
                new AuctionPreview(
                    1,
                    1,
                    "Item Name",
                    "img.png",
                    ItemType.ART,
                    AuctionStatus.OPEN,
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now().plusDays(2),
                    1000,
                    1000,
                    100,
                    1,
                    new UserPreview(5, "SellerName", "seller_user", UserRole.SELLER, "avatar.png"));
            AuctionStore.getInstance().addPreview(preview);

            assertDoesNotThrow(controller::initialize);
            assertDoesNotThrow(() -> controller.handleRefreshAuctions(new ActionEvent()));
            assertDoesNotThrow(() -> controller.handleRefreshUsers(new ActionEvent()));
            assertDoesNotThrow(() -> controller.handleLogout(new ActionEvent()));
            assertDoesNotThrow(controller::handleClearForm);

            assertDoesNotThrow(() -> controller.handleCreateAuction(new ActionEvent()));

            setPrivateField(controller, "actionLoading", true);
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "handleIncomingMessage",
                        new Class<?>[] {String.class},
                        new Object[] {"OK"}));

            // Cover private helper methods
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "setupGreeter", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "setupAuctionsTable", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () -> invokePrivateMethod(controller, "setupForm", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "setupSearchFilters", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "autofillForm",
                        new Class<?>[] {AuctionPreview.class},
                        new Object[] {preview}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "loadAuctionsData", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "getStatusScore",
                        new Class<?>[] {AuctionStatus.class},
                        new Object[] {AuctionStatus.RUNNING}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "filterAuctions",
                        new Class<?>[] {String.class},
                        new Object[] {"test"}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "filterUsers",
                        new Class<?>[] {String.class},
                        new Object[] {"test"}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "setActionLoading",
                        new Class<?>[] {boolean.class},
                        new Object[] {true}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "setActionLoading",
                        new Class<?>[] {boolean.class},
                        new Object[] {false}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "loadUsersData",
                        new Class<?>[0],
                        new Object[0]));

            assertDoesNotThrow(controller::cleanup);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  public void testLiveController() throws Exception {
    runOnFxThread(
        () -> {
          try {
            LiveController controller = new LiveController();

            Label titerTimer = new Label();
            Label timeLabel = new Label();
            Label itemNameLabel = new Label();
            Label descriptionLabel = new Label();
            Label sellerNameLabel = new Label();
            Label statusBadge = new Label();
            Label startPriceLabel = new Label();
            Label stepPriceLabel = new Label();
            Label currentPriceLabel = new Label();
            Label bidCountLabel = new Label();
            Label statusLabel = new Label();
            Label bidHintLabel = new Label();
            Label availableBalanceLabel = new Label();
            Label leaderNameLabel = new Label();
            Label leaderPriceLabel = new Label();
            Circle leaderAvatar = new Circle();
            TextField bidAmountField = new TextField();
            ScrollPane bidHistoryScrollPane = new ScrollPane();
            VBox bidHistoryList = new VBox();
            ProgressIndicator detailLoadingIndicator = new ProgressIndicator();
            ImageView itemImageView = new ImageView();
            Label imagePlaceholderLabel = new Label();
            StackPane priceChartContainer = new StackPane();
            Canvas priceChartCanvas = new Canvas();
            TextField autoBidMaxField = new TextField();
            TextField autoBidStepField = new TextField();
            Label autoBidStatusLabel = new Label();
            Button setAutoBidBtn = new Button();
            Button disableAutoBidBtn = new Button();

            setPrivateField(controller, "titerTimer", titerTimer);
            setPrivateField(controller, "timeLabel", timeLabel);
            setPrivateField(controller, "itemNameLabel", itemNameLabel);
            setPrivateField(controller, "descriptionLabel", descriptionLabel);
            setPrivateField(controller, "sellerNameLabel", sellerNameLabel);
            setPrivateField(controller, "statusBadge", statusBadge);
            setPrivateField(controller, "startPriceLabel", startPriceLabel);
            setPrivateField(controller, "stepPriceLabel", stepPriceLabel);
            setPrivateField(controller, "currentPriceLabel", currentPriceLabel);
            setPrivateField(controller, "bidCountLabel", bidCountLabel);
            setPrivateField(controller, "statusLabel", statusLabel);
            setPrivateField(controller, "bidHintLabel", bidHintLabel);
            setPrivateField(controller, "availableBalanceLabel", availableBalanceLabel);
            setPrivateField(controller, "leaderNameLabel", leaderNameLabel);
            setPrivateField(controller, "leaderPriceLabel", leaderPriceLabel);
            setPrivateField(controller, "leaderAvatar", leaderAvatar);
            setPrivateField(controller, "bidAmountField", bidAmountField);
            setPrivateField(controller, "bidHistoryScrollPane", bidHistoryScrollPane);
            setPrivateField(controller, "bidHistoryList", bidHistoryList);
            setPrivateField(controller, "detailLoadingIndicator", detailLoadingIndicator);
            setPrivateField(controller, "itemImageView", itemImageView);
            setPrivateField(controller, "imagePlaceholderLabel", imagePlaceholderLabel);
            setPrivateField(controller, "priceChartContainer", priceChartContainer);
            setPrivateField(controller, "priceChartCanvas", priceChartCanvas);
            setPrivateField(controller, "autoBidMaxField", autoBidMaxField);
            setPrivateField(controller, "autoBidStepField", autoBidStepField);
            setPrivateField(controller, "autoBidStatusLabel", autoBidStatusLabel);
            setPrivateField(controller, "setAutoBidBtn", setAutoBidBtn);
            setPrivateField(controller, "disableAutoBidBtn", disableAutoBidBtn);

            User mockUser =
                new User(
                    1,
                    "Test User",
                    new Account("test", "pass", UserRole.BIDDER),
                    new Wallet(BigDecimal.TEN));
            UserManager.getInstance().setCurrentUser(mockUser);

            AuctionPreview preview =
                new AuctionPreview(
                    1,
                    1,
                    "Item Name",
                    "img.png",
                    ItemType.ART,
                    AuctionStatus.RUNNING,
                    LocalDateTime.now().minusDays(1),
                    LocalDateTime.now().plusDays(1),
                    2000,
                    1000,
                    100,
                    1,
                    new UserPreview(5, "SellerName", "seller_user", UserRole.SELLER, "avatar.png"));
            AuctionStore.getInstance().addPreview(preview);
            LiveAuctionSessionStore.getInstance().selectAuction(preview);

            assertDoesNotThrow(controller::initialize);

            Auction mockAuction = new Auction(1, 5, LocalDateTime.now().plusDays(1), 2000L);
            mockAuction.setId(1);
            mockAuction.setStatus(AuctionStatus.RUNNING);
            mockAuction.setStartTime(LocalDateTime.now().minusDays(1));
            mockAuction.setEndTime(LocalDateTime.now().plusDays(1));
            mockAuction.setHighestBid(2000);
            Item mockItem =
                app.common.models.ItemFactory.createItem(
                    1, "Item Name", 5, "Item Desc", 1000L, 100L, ItemType.ART);
            mockAuction.setItem(mockItem);
            mockAuction.setBids(new ArrayList<>());

            assertDoesNotThrow(() -> controller.setAuction(mockAuction));

            Button btn = new Button();
            btn.setUserData("100");
            assertDoesNotThrow(() -> controller.handleQuickBid(new ActionEvent(btn, null)));

            // Cover private helper methods
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "updateAutoBidUi", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "updateAvailableBalance", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(controller, "updateTimer", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "drawPriceChart", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "updateBidCount",
                        new Class<?>[] {int.class},
                        new Object[] {5}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "updateBidHint", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "updateLeaderEmpty", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "updateSellerName",
                        new Class<?>[] {AuctionPreview.class},
                        new Object[] {preview}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "updateStatusBadge",
                        new Class<?>[] {AuctionStatus.class},
                        new Object[] {AuctionStatus.RUNNING}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "setBidLoading",
                        new Class<?>[] {boolean.class},
                        new Object[] {true}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "setBidLoading",
                        new Class<?>[] {boolean.class},
                        new Object[] {false}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller, "refreshDetailFromStore", new Class<?>[0], new Object[0]));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "applyPreview",
                        new Class<?>[] {AuctionPreview.class},
                        new Object[] {preview}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "applyDetail",
                        new Class<?>[] {Auction.class},
                        new Object[] {mockAuction}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "updateBidHistory",
                        new Class<?>[] {Auction.class},
                        new Object[] {mockAuction}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "updateLeaderInfo",
                        new Class<?>[] {Auction.class},
                        new Object[] {mockAuction}));
            assertDoesNotThrow(
                () ->
                    invokePrivateMethod(
                        controller,
                        "updateSellerInfo",
                        new Class<?>[] {Auction.class},
                        new Object[] {mockAuction}));

            assertDoesNotThrow(controller::cleanup);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }
}
