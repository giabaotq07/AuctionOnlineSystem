package app.server.config;

public final class config {
  private final String url;
  private final String user;
  private final String password;

  private config(String url, String user, String password) {
   this.url = url;
   this.user = user;
   this.password = password;
  }

  public static config load() {
   String url = System.getProperty("db.url", "jdbc:mysql://localhost:3306/auction_db");
   String user = System.getProperty("db.user", "root");
   String pass = System.getProperty("db.password", "123456"); // password
   return new config(url, user, pass);
  }

  public String getUrl() {
   return url;
  }

  public String getUser() {
   return user;
  }

  public String getPassword() {
   return password;
  }
}

