package app.models;

import app.enums.Result;
import java.io.Serializable;

public class ResponsePacket<T> implements Serializable {
  private Result type;
  private String message;
  private T data;

  public ResponsePacket(Result type, T data) {
    this.type = type;
    this.data = data;
  }

  public void setType(Result type) {
    this.type = type;
  }

  public void setMessage(String message) {
            this.message = message;
  }

  public void setData(T data) {
    this.data = data;
  }

  public Result getType() {
    return type;
  }

  public T getData() {
    return data;
  }

  public String getMessage() {
    return message;
  }
}
