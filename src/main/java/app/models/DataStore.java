package app.models;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DataStore implements Serializable {
  public static final List<Auction> sessions = new CopyOnWriteArrayList<>();
  public static User currentUser; // Track the logged-in user
}
