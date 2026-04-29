package app.models;

public abstract class Entity implements java.io.Serializable {
  protected int id;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }
}
