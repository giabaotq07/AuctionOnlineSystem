package Server;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

/** Lớp Client để kết nối và gửi dữ liệu tới Server. */
public class Client {
  private Socket socket;
  private ObjectOutputStream output;
  private static volatile Client instance;

  private Client() throws IOException {
    socket = new Socket("127.0.0.1", Server.PORT);
    System.out.println("Đã kết nối tới Server tại " + socket.getInetAddress());
    output = new ObjectOutputStream(socket.getOutputStream());
  }

  public static Client getInstance() throws IOException {
    if (instance == null) {
      synchronized (Client.class) {
        if (instance == null) {
          instance = new Client();
        }
      }
    }
    return instance;
  }

  public ObjectOutputStream getOutput() {
    return output;
  }

  public Socket getSocket() {
    return socket;
  }
}
