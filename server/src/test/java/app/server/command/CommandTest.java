package app.server.command;

import static org.junit.jupiter.api.Assertions.*;

import app.TestFixtures;
import app.common.dto.*;
import app.common.enums.*;
import app.common.models.*;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.dao.BaseDAOTest;
import app.server.dao.UserDAO;
import app.server.dao.impl.MySqlUserDAO;
import app.server.database.TransactionManager;
import app.server.network.ClientHandler;
import app.server.network.Session;
import app.server.service.UserService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Lop kiem thu cho cac Command phia Server. Su dung FakeClientHandler tu che va H2 database thuc te
 * de tranh loi kien truc Mockito tren Java 25. Viet bang tieng Viet khong dau de de dang giai
 * thich.
 */
public class CommandTest extends BaseDAOTest {

  private FakeClientHandler fakeClientHandler;
  private UserService userService;
  private UserDAO userDAO;
  private TransactionManager transactionManager;

  /** Thiet lap moi truong H2 DB va khoi tao real UserService phuc vu integration test. */
  @BeforeEach
  public void setUp() {
    userDAO = new MySqlUserDAO();
    transactionManager = new TransactionManager();
    userService = new UserService(userDAO, transactionManager);

    // Dung FakeClientHandler khong co socket thuc te
    fakeClientHandler = new FakeClientHandler();
  }

  /** Test LoginCommand dang nhap thanh cong voi thong tin chinh xac. */
  @Test
  public void testLoginCommandSuccess() {
    // Luu san user vao DB qua UserService
    User rawUser = TestFixtures.user("test_command_user", UserRole.BIDDER, new BigDecimal("5000"));
    User registered = userService.register(rawUser);

    LoginCommand loginCmd = new LoginCommand(userService);
    LoginRequest payload =
        new LoginRequest(
            "test_command_user", "password"); // TestFixtures mac dinh tao pass la "password"
    PacketReq reqPacket = PacketReq.of(RequestType.LOGIN, payload);

    loginCmd.execute(fakeClientHandler, reqPacket);

    // Kiem tra phien dang nhap phai duoc authenticate va luu thong tin dung user
    assertTrue(fakeClientHandler.getSession().isAuthenticated());
    assertEquals(registered.getId(), fakeClientHandler.getSession().getUser().getId());

    // Kiem tra goi tin phan hoi dang nhap thanh cong
    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertTrue(res.isSuccess());
    assertEquals(ResponseType.LOGIN_RESULT, res.getType());
    assertEquals("Đăng nhập thành công!", res.getMessage());

    LoginResponse data = res.getData(LoginResponse.class);
    assertNotNull(data);
    assertEquals(registered.getName(), data.user().name());
  }

  /** Test LoginCommand that bai khi thieu thong tin dang nhap. */
  @Test
  public void testLoginCommandMissingCredentials() {
    LoginCommand loginCmd = new LoginCommand(userService);
    LoginRequest payload = new LoginRequest("", "   ");
    PacketReq reqPacket = PacketReq.of(RequestType.LOGIN, payload);

    loginCmd.execute(fakeClientHandler, reqPacket);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertFalse(res.isSuccess());
    assertEquals("Tên đăng nhập và mật khẩu không được để trống.", res.getMessage());
  }

  /** Test LoginCommand that bai khi sai mật khẩu. */
  @Test
  public void testLoginCommandWrongPassword() {
    User rawUser = TestFixtures.user("test_command_user", UserRole.BIDDER, new BigDecimal("5000"));
    userService.register(rawUser);

    LoginCommand loginCmd = new LoginCommand(userService);
    LoginRequest payload = new LoginRequest("test_command_user", "wrong_pass");
    PacketReq reqPacket = PacketReq.of(RequestType.LOGIN, payload);

    loginCmd.execute(fakeClientHandler, reqPacket);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertFalse(res.isSuccess());
    assertEquals("Tên đăng nhập hoặc mật khẩu không đúng", res.getMessage());
  }

  /** Test RegisterCommand dang ky thanh cong. */
  @Test
  public void testRegisterCommandSuccess() {
    RegisterCommand regCmd = new RegisterCommand(userService);
    RegisterRequest payload =
        new RegisterRequest("Anh Tu", "anhtu123", "mypass123", UserRole.BIDDER);
    PacketReq reqPacket = PacketReq.of(RequestType.REGISTER, payload);

    regCmd.execute(fakeClientHandler, reqPacket);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertTrue(res.isSuccess());
    assertEquals(ResponseType.REGISTER_RESULT, res.getType());
    assertEquals("Đăng ký thành công!", res.getMessage());

    RegisterResponse data = res.getData(RegisterResponse.class);
    assertNotNull(data);
    assertEquals("Anh Tu", data.user().name());
    assertEquals("anhtu123", data.user().account().username());
  }

  /** Test RegisterCommand that bai do trung username da dang ky. */
  @Test
  public void testRegisterCommandDuplicateUsername() {
    // Dang ky truoc 1 lan
    User rawUser = TestFixtures.user("duplicate_user", UserRole.BIDDER);
    userService.register(rawUser);

    RegisterCommand regCmd = new RegisterCommand(userService);
    RegisterRequest payload =
        new RegisterRequest("Nguyen Van B", "duplicate_user", "mypass123", UserRole.BIDDER);
    PacketReq reqPacket = PacketReq.of(RequestType.REGISTER, payload);

    regCmd.execute(fakeClientHandler, reqPacket);

    PacketRes res = fakeClientHandler.getSentPacket();
    assertNotNull(res);
    assertFalse(res.isSuccess());
    assertEquals("User đã tồn tại: duplicate_user", res.getMessage());
  }

  /** FakeClientHandler de gia lap hoat dong socket network va phien session. */
  public static class FakeClientHandler extends ClientHandler {
    private final Session fakeSession = new Session();
    private PacketRes sentPacket;

    public FakeClientHandler() {
      // Pass null de khong can phai setup socket connections that
      super(null, null, null, null, null, null, null);
    }

    @Override
    public Session getSession() {
      return fakeSession;
    }

    @Override
    public boolean isAuthenticated() {
      return fakeSession.isAuthenticated();
    }

    @Override
    public User getUser() {
      return fakeSession.getUser();
    }

    @Override
    public void sendPacket(PacketRes packet) {
      this.sentPacket = packet;
    }

    public PacketRes getSentPacket() {
      return sentPacket;
    }
  }
}
