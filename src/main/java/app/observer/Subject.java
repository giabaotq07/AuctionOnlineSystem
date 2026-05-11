package app.observer;

public interface Subject<T> {
  void registerObserver(T observer);

  void removeObserver(T observer);
}
