package app.models;

public abstract class User extends Entity {
  private String username;
  private String password;
  private String fullName;
  private String role; // Có thể dùng Enum: BIDDER, SELLER, ADMIN [cite: 34]

  public User(String username, String password, String fullName, String role) {
    super(); // Gọi constructor của Entity
    this.username = username;
    this.password = password;
    this.fullName = fullName;
    this.role = role;
  }

  @Override
  public void printInfo() {
    System.out.println("User ID: " + id + " | Username: " + username + " | Role: " + role);
  }

  // Getters & Setters
  public String getUsername() {
    return username;
  }

  public String getRole() {
    return role;
  }
}
