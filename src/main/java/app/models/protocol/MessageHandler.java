package app.models.protocol;

public interface MessageHandler<T> {
  void messageHandlerReceiver(T data);
}
