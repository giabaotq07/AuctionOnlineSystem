package app;

import app.config.DatabaseConnection;
import app.network.Server;

public class ServerApp {
  public static void main(String[] args) {
    DatabaseConnection.initializeDatabase();
    Server.getInstance().start();
  }
}
