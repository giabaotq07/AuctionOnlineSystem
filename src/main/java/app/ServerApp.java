package app;

import app.network.Server;

public class ServerApp {
  public static void main(String[] args) {
    Server.getInstance().start();
  }
}
