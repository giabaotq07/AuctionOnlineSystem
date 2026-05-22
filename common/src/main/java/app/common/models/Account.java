package app.common.models;

import app.common.enums.UserRole;

/** Account. */
public class Account {
  private String username;
  private transient String password;
  private UserRole role;

  /** Account. */
  public Account(String username, String password) {
    this.username = username;
    this.password = password;
  }

  public Account(String username, String password, UserRole role) {
    this.username = username;
    this.password = password;
    this.role = role;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public UserRole getRole() {
    return role;
  }

  public void setRole(UserRole role) {
    this.role = role;
  }
}
