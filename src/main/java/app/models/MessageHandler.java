package app.models;

public interface MessageHandler<T> {
  void messageHandlerReceiver(T data);
}
