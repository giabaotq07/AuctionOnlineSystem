package app.models;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DataStore implements java.io.Serializable {
  public static final List<AuctionSession> sessions = new CopyOnWriteArrayList<>();
  public static User currentUser; // Track the logged-in user
}
