package app.observer;

public interface UpdateSubject extends Subject<Object> {
  void notifyObserverUpdate();
}
