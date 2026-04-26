package app.services;

import app.dao.HistoryDAO;
import app.models.HistoryRecord;
import app.models.HistoryType;
import java.util.List;

public class HistoryService {
  private final HistoryDAO historyDAO;

  public HistoryService(HistoryDAO historyDAO) {
    this.historyDAO = historyDAO;
  }

  public boolean logEvent(int sessionId, HistoryType type, String message) {
    HistoryRecord record = new HistoryRecord(sessionId, type, message);
    return historyDAO.addHistoryRecord(record);
  }

  public List<HistoryRecord> getSessionHistory(int sessionId) {
    return historyDAO.getHistoryBySession(sessionId);
  }
}
