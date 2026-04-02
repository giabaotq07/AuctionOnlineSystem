package Common;

public class Seller extends User {

  public Seller(String username, String password, String fullName) {
    super(username, password, fullName, "SELLER");
  }

  public void createItem(Item item) {
    System.out.println("Seller " + getUsername() + " created item: " + item.getItemName());
  }

  public void removeItem(Item item) {
    System.out.println("Seller removed item: " + item.getItemName());
  }

  @Override
  public void printInfo() {
    super.printInfo();
    System.out.println("Role: Seller");
  }
}
