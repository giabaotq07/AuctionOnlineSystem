package app.server.command;

import app.common.dto.Request;
import app.common.enums.ResponseType;
import app.common.exception.DatabaseException;
import app.common.exception.ServiceException;
import app.common.exception.ValidationException;
import app.common.models.User;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Template-method command boundary with centralized exception mapping. */
public abstract class SafeCommand implements Command {
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public final void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      doExecute(clientHandler, packet);
    } catch (ServiceException e) {
      logger.warn("{} failed: {}", getClass().getSimpleName(), e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (DatabaseException e) {
      logger.error("{} database error", getClass().getSimpleName(), e);
      sendError(clientHandler, databaseErrorMessage());
    } catch (JsonSyntaxException e) {
      logger.warn("{} received malformed JSON: {}", getClass().getSimpleName(), e.getMessage());
      sendError(clientHandler, invalidRequestMessage());
    } catch (IllegalArgumentException e) {
      logger.warn("{} validation error: {}", getClass().getSimpleName(), e.getMessage());
      sendError(clientHandler, messageOrDefault(e.getMessage(), invalidRequestMessage()));
    } catch (IOException e) {
      logger.error("{} IO error", getClass().getSimpleName(), e);
      sendError(clientHandler, ioErrorMessage());
    } catch (Exception e) {
      logger.error("{} unexpected error", getClass().getSimpleName(), e);
      sendError(clientHandler, unexpectedErrorMessage());
    }
  }

  protected abstract void doExecute(ClientHandler clientHandler, PacketReq packet) throws Exception;

  protected abstract ResponseType responseType();

  protected String unexpectedErrorMessage() {
    return "Không thể thực hiện yêu cầu.";
  }

  protected String invalidRequestMessage() {
    return "Dữ liệu yêu cầu không hợp lệ.";
  }

  protected String ioErrorMessage() {
    return "Lỗi đọc/ghi dữ liệu trên server.";
  }

  protected String databaseErrorMessage() {
    return "Lỗi dữ liệu hoặc kết nối, vui lòng thử lại.";
  }

  protected <T extends Request> T requirePayload(PacketReq packet, Class<T> clazz, String message) {
    T payload = packet == null ? null : packet.getData(clazz);
    if (payload == null) {
      throw new ValidationException(message);
    }
    return payload;
  }

  protected User requireUser(ClientHandler clientHandler) {
    User user = clientHandler == null ? null : clientHandler.getUser();
    if (user == null || user.getId() <= 0) {
      throw new ValidationException("Người dùng chưa đăng nhập hoặc không hợp lệ.");
    }
    return user;
  }

  protected void sendSuccess(ClientHandler clientHandler, String message, Object payload) {
    clientHandler.sendPacket(PacketRes.of(responseType(), message, payload));
  }

  protected void sendError(ClientHandler clientHandler, String message) {
    if (clientHandler != null) {
      clientHandler.sendPacket(PacketRes.error(responseType(), message));
    }
  }

  private String messageOrDefault(String message, String defaultMessage) {
    return message == null || message.isBlank() ? defaultMessage : message;
  }
}
