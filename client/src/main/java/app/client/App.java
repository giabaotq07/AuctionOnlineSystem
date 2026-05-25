package app.client;

import javafx.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** run app */
public class App {
  private static final Logger logger = LoggerFactory.getLogger(App.class);

  public static void main(String[] args) {
    Application.launch(Main.class, args);
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  logger.info("Received shutdown signal");
                  Client.getInstance().closeResources();
                }));
  }
}
