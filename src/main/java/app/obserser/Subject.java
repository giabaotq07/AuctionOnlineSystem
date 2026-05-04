package app.obserser;

public interface Subject {
  void registerObserver(Observer observer);

  void removeObserver(Observer observer);

  void notifyObserversNewBid(long price, String bidderName);
}
