package app.obserser;

public interface Subject {
  void registerObserver(Observer observer);

  void removeObserver(Observer observer);

  void notifyObserversNewBid(double price, String bidderName);
}
