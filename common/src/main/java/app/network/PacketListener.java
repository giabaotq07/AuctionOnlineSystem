package app.network;

import app.dto.Response;

/** PacketListener. */
public interface PacketListener<T extends Response> {
  /** handle. */
  void handle(T packet, boolean success, String message);
}
