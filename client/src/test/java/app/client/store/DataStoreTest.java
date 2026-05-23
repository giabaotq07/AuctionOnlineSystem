package app.client.store;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** DataStoreTest. Kiem thu lop abstract DataStore de dat 100% do phu. */
public class DataStoreTest {

  @Test
  public void testDataStore() {
    DataStore ds = new DataStore() {};
    assertNotNull(ds);
  }
}
