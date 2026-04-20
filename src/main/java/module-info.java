module app {
  requires javafx.controls;
  requires javafx.fxml;
  requires java.desktop;
  requires atlantafx.base;
  requires java.sql;
    requires com.google.gson;

    opens app to
      javafx.fxml;
  opens app.controllers to
      javafx.fxml;
  opens app.models to com.google.gson;

  exports app;
}
