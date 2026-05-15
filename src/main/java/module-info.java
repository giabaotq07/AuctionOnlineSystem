module app {
  requires javafx.controls;
  requires javafx.fxml;
  requires transitive javafx.graphics;
  requires transitive java.sql;
  requires org.slf4j;
  requires ch.qos.logback.classic;
  requires com.google.gson;
  requires atlantafx.base;
  requires transitive com.zaxxer.hikari;

  opens app.dto;
  opens app.dao;
  opens app.service;
  opens app to
      javafx.fxml;
  opens app.controllers to
      javafx.fxml;
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
  exports app.dto;
  exports app.dao;
  exports app.database;
}
