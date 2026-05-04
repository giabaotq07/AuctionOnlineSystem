package app.models;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HistoryStore implements java.io.Serializable {
  public static final List<HistoryRecord> history = new CopyOnWriteArrayList<>();
}
