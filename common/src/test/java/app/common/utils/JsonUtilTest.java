package app.common.utils;

import static org.junit.jupiter.api.Assertions.*;

import app.common.exception.AppException;
import org.junit.jupiter.api.Test;

/** Lop kiem thu cho JsonUtil. Viet bang tieng Viet khong dau de mentor de dang giai thich. */
public class JsonUtilTest {

  /** Test chuyen doi tuong sang chuoi JSON. */
  @Test
  public void testToJson() {
    TestObj obj = new TestObj("Antigravity", 25);
    String json = JsonUtil.toJson(obj);
    assertNotNull(json);
    assertTrue(json.contains("Antigravity"));
    assertTrue(json.contains("25"));
  }

  /** Test chuyen doi chuoi JSON sang doi tuong thanh cong. */
  @Test
  public void testFromJsonSuccess() {
    String json = "{\"name\":\"Antigravity\",\"age\":25}";
    TestObj obj = JsonUtil.fromJson(json, TestObj.class);
    assertNotNull(obj);
    assertEquals("Antigravity", obj.getName());
    assertEquals(25, obj.getAge());
  }

  /** Test chuyen doi chuoi JSON loi, ky vong ném ra AppException. */
  @Test
  public void testFromJsonSyntaxError() {
    String invalidJson = "{invalid_json"; // Hoan toan sai cu phap JSON
    assertThrows(
        AppException.class,
        () -> {
          JsonUtil.fromJson(invalidJson, TestObj.class);
        });
  }

  /** Test tra ve doi tuong Gson. */
  @Test
  public void testGson() {
    assertNotNull(JsonUtil.gson());
  }

  /** Lop helper phuc vu kiem thu. */
  public static class TestObj {
    private String name;
    private int age;

    public TestObj(String name, int age) {
      this.name = name;
      this.age = age;
    }

    public String getName() {
      return name;
    }

    public int getAge() {
      return age;
    }
  }
}
