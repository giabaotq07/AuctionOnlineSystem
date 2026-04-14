package Server.dao;

import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
  private static Properties props = new Properties();

  static {
    try (InputStream input =
        AppConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
      props.load(input);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static String get(String key) {
    return props.getProperty(key);
  }
}
