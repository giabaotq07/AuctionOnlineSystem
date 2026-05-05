package app;

import app.config.DatabaseConnection;
import javafx.application.Application;

public class App {
  public static void main(String[] args) {
    DatabaseConnection.initializeDatabase();
    Application.launch(Main.class, args);
  }
}
/