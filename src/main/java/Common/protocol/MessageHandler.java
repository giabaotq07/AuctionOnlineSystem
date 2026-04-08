package Common.protocol;

public interface MessageHandler<T> {
  void messageHandlerReceiver(T data);
}
