package app.common.protocol;

import static org.junit.jupiter.api.Assertions.*;

import app.common.dto.LoginRequest;
import app.common.dto.LoginResponse;
import app.common.dto.UserDto;
import app.common.enums.RequestType;
import app.common.enums.ResponseType;
import org.junit.jupiter.api.Test;

/** Lop kiem thu cho PacketReq va PacketRes. Viet bang tieng Viet khong dau de giai thich. */
public class PacketTest {

  /** Test khoi tao va trich xuat du lieu tu PacketReq. */
  @Test
  public void testPacketReq() {
    LoginRequest reqPayload = new LoginRequest("test_user", "test_pass");
    PacketReq req = PacketReq.of(RequestType.LOGIN, reqPayload);

    assertNotNull(req);
    assertEquals(RequestType.LOGIN, req.getType());

    LoginRequest extracted = req.getData(LoginRequest.class);
    assertNotNull(extracted);
    assertEquals("test_user", extracted.username());
    assertEquals("test_pass", extracted.password());

    // Truong hop dac biet
    assertNull(req.getData(null));

    PacketReq reqNull = new PacketReq(RequestType.LOGIN, null);
    assertNull(reqNull.getData(LoginRequest.class));
  }

  /** Test khoi tao va trich xuat du lieu tu PacketRes. */
  @Test
  public void testPacketRes() {
    UserDto userDto = new UserDto(1, "Test A", null, null, null);
    LoginResponse resPayload = new LoginResponse(userDto);
    PacketRes res = PacketRes.of(ResponseType.LOGIN_RESULT, "Success", resPayload);

    assertNotNull(res);
    assertTrue(res.isSuccess());
    assertEquals(ResponseType.LOGIN_RESULT, res.getType());
    assertEquals("Success", res.getMessage());

    LoginResponse extracted = res.getData(LoginResponse.class);
    assertNotNull(extracted);
    assertNotNull(extracted.user());
    assertEquals("Test A", extracted.user().name());

    // Truong hop loi
    PacketRes errorRes = PacketRes.error(ResponseType.ERROR, "Invalid Action");
    assertNotNull(errorRes);
    assertFalse(errorRes.isSuccess());
    assertEquals(ResponseType.ERROR, errorRes.getType());
    assertEquals("Invalid Action", errorRes.getMessage());
    assertNull(errorRes.getData(LoginResponse.class));

    // To String check
    String jsonStr = res.toString();
    assertNotNull(jsonStr);
    assertTrue(jsonStr.contains("LOGIN_RESULT"));

    // Truong hop dac biet
    assertNull(res.getData(null));
  }
}
