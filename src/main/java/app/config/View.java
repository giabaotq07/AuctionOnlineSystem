package app.config;

public enum View {
  CONNECT("/app/views/ConnectServerController.fxml"),
  LOGIN("/app/views/login_scene.fxml"),
  ORGANIZE("/app/views/hold_an_auction.fxml"),
  LIVE("/app/views/live_auction.fxml"),
  MINE("/app/views/my_auction.fxml"),
  REGISTER("/app/views/register_account.fxml"),
  UI("/app/views/user_interface.fxml"),
  MESSAGE("/app/views/mess_chat.fxml"),
  ;

  private final String fxmlPath;

  View(String fxmlPath) {
    this.fxmlPath = fxmlPath;
  }

  public String getFxmlPath() {
    return fxmlPath;
  }
}
