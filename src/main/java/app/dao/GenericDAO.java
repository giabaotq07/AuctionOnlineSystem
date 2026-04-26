package app.dao;

import java.util.List;

public interface GenericDAO<T, ID> {
  T getById(ID id);
  List<T> getAll();
  T add(T entity);
  boolean update(T entity);
  boolean delete(ID id);
}

