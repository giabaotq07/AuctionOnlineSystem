module app.client {
  requires atlantafx.base;
  requires transitive app.common;
  requires transitive javafx.controls;
  requires javafx.fxml;
  requires org.slf4j;

  exports app.client;
  exports app.client.command;
  exports app.client.controllers;
  exports app.client.manager;
  exports app.client.store;
  exports app.client.utils;

  opens app.client.controllers to
      javafx.fxml;
}
