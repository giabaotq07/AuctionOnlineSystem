package app.server.dao;

import app.models.Notification;
import java.util.List;

public interface NotificationDAO {
  int save(Notification notification);

  List<Notification> findByUser(int userId);
}

