module app {
  requires javafx.controls;
  requires javafx.fxml;
  requires java.desktop;
  requires atlantafx.base;

  opens app to
      javafx.fxml;
  opens app.controllers to
      javafx.fxml;

  exports app;
}
