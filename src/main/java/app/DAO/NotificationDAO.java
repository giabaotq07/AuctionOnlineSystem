package app.DAO;

import app.models.Notification;
import java.util.List;

/** NotificationDAO. */
public interface NotificationDAO {
  /** save. */
  int save(Notification notification);

  /** findByUser. */
  List<Notification> findByUser(int userId);
}
