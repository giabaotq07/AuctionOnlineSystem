package app.network;

/** PacketListener. */
public interface PacketListener<T> {
  /** handle. */
  void handle(T packet);
}
