package app.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public abstract class Entity implements Serializable {
  protected int id;
  protected LocalDateTime createdAt;

  public Entity(int id) {
    this.id = id;
    this.createdAt = LocalDateTime.now();
  }

  public Entity() {
    this.createdAt = LocalDateTime.now();
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
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
