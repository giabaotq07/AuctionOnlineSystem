package app.dao;

import app.models.Notification;
import java.util.List;

/** NotificationDao. */
public interface NotificationDao {
  /** save. */
  int save(Notification notification);

  /** findByUser. */
  List<Notification> findByUser(int userId);
}
