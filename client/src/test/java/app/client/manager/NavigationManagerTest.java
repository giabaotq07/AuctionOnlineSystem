package app.client.manager;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class NavigationManagerTest {

  @Test
  public void testSingleton() {
    NavigationManager m1 = NavigationManager.getInstance();
    NavigationManager m2 = NavigationManager.getInstance();
    assertNotNull(m1);
    assertSame(m1, m2);
  }

  @Test
  public void testOpenAuctionDetailNull() {
    assertDoesNotThrow(() -> NavigationManager.getInstance().openAuctionDetail(null));
  }

  @Test
  public void testSetPrimaryStageNull() {
    assertDoesNotThrow(() -> NavigationManager.getInstance().setPrimaryStage(null));
  }
}
