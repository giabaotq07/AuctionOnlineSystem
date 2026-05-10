package app.models;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HistoryStore {
  public static final List<HistoryRecord> history = new CopyOnWriteArrayList<>();
}
