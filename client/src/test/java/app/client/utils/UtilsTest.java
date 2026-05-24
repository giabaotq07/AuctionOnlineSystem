package app.client.utils;

import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * UtilsTest. Kiem thu cac lop tien ich phia Client. Su dung doi tuong ActionEvent va Button thuc
 * te.
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
  public void testLoadingButton() {
    // Su dung doi tuong Button thuc te thay vi Mock de dam bao tuong thich 100% JDK 25
    Button button = new Button("Submit");

    // 1. Test fromEvent
    assertNull(LoadingButton.fromEvent(null));

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
