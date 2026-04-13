package app.models;

public class Admin extends User {

  public Admin(String username, String password, String fullName) {
    super(username, password, fullName, "ADMIN");
  }

  public void banUser(User user) {
    System.out.println("Admin banned user: " + user.getUsername());
  }

  public void viewAllUsers() {
    System.out.println("Admin is viewing all users...");
  }

  @Override
  public void printInfo() {
    super.printInfo();
    System.out.println("Role: Admin");
  }
}
