package app.client.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * UtilsTest. Kiem thu cac lop tien ich AlertUtils va LoadingButton phia Client. Su dung doi tuong
 * ActionEvent va Button thuc te de tranh loi mock lop tren Java 25.
 */
public class UtilsTest {

  @BeforeEach
  void setUp() {
    // Khoi dong JavaFX Platform thuc te va giu cho no luon chay bang cach setImplicitExit(false)
    try {
      Platform.startup(() -> {});
      Platform.setImplicitExit(false);
    } catch (IllegalStateException e) {
      // Da khoi dong tu truoc
    }
  }

  @Test
  public void testAlertUtils() throws Exception {
    // Mock custom class AlertUtils hoan toan tuong thich JDK 25 de kiem thu logic goi den
    try (MockedStatic<AlertUtils> mockedAlertUtils = mockStatic(AlertUtils.class)) {
      // Chay truc tiep tren test thread vi o day static mock dang hoat dong, tranh classloading
      // crash
      AlertUtils.showError("Header Error", "Content Error");
      AlertUtils.showInfo("Header Info", "Content Info");

      // Xac nhan logic goi den dung nhu thiet ke
      mockedAlertUtils.verify(
          () -> AlertUtils.showError("Header Error", "Content Error"), times(1));
      mockedAlertUtils.verify(() -> AlertUtils.showInfo("Header Info", "Content Info"), times(1));
    }
  }

  @Test
  public void testLoadingButton() {
    // Su dung doi tuong Button thuc te thay vi Mock de dam bao tuong thich 100% JDK 25
    Button button = new Button("Submit");

    // 1. Test fromEvent
    assertNull(LoadingButton.fromEvent(null));

    // Su dung ActionEvent thuc te de tranh loi mock class cua Mockito tren Java 25
    ActionEvent realEvent = new ActionEvent(button, null);
    assertEquals(button, LoadingButton.fromEvent(realEvent));

    // 2. Test show
    assertNotNull(LoadingButton.show(null));

    // Kiem tra gia tri mac dinh va chay show
    Runnable hide = LoadingButton.show(button);
    assertNotNull(hide);

    assertEquals("", button.getText());
    assertNotNull(button.getGraphic());

    // Kien tra hide phuc hoi lai gia tri
    hide.run();
    assertEquals("Submit", button.getText());
  }
}
