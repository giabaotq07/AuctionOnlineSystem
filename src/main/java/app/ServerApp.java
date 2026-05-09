package app;

import app.network.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerApp {

  private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);

  static void main(String[] args) {
    Server server = Server.getInstance();

    // Register shutdown hook để graceful shutdown khi Ctrl+C
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  logger.info("Received shutdown signal");
                  server.shutdown();
                }));

    server.start();
  }
}
