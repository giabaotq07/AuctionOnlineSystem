module app {
  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.graphics;
  requires java.sql;
  requires org.slf4j;
  requires ch.qos.logback.classic;
  requires com.google.gson;
  requires atlantafx.base;

  opens app to
      javafx.fxml;
  opens app.controllers to
      javafx.fxml;
  opens app.data to
      com.google.gson;
  opens app.models to
      com.google.gson;
  opens app.enums to
      com.google.gson;

  exports app;
  exports app.controllers to
      javafx.fxml;
  exports app.service;
  exports app.models;
  exports app.enums;
}
