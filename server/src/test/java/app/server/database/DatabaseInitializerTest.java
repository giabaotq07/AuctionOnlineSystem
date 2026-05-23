package app.server.database;

import static org.junit.jupiter.api.Assertions.*;

import app.server.dao.BaseDAOTest;
import org.junit.jupiter.api.Test;

/** Lop kiem thu cho DatabaseInitializer. Viet bang tieng Viet khong dau theo dung quy dinh. */
public class DatabaseInitializerTest extends BaseDAOTest {

  @Test
  public void testInitializeSuccess() {
    // Kiem tra ham initialize chay khong nem ra loi
    assertDoesNotThrow(() -> DatabaseInitializer.initialize());
  }

  @Test
  public void testMainSuccess() {
    // Kiem tra ham main chay thanh cong
    assertDoesNotThrow(() -> DatabaseInitializer.main());
  }
}
