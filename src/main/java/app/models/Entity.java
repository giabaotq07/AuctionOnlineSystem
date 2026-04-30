package app.models;

import java.util.Objects;

public abstract class Entity implements java.io.Serializable {
  protected int id;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Entity entity = (Entity) o;
    return id == entity.id;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
