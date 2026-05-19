package app.common.models;

/** Profile. */
public class Profile {
  private String name;

  /** Profile. */
  public Profile(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
