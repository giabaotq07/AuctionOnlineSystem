package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/** Lớp Server để lắng nghe và chấp nhận các kết nối từ Client. */
public class Server {
  /** Cổng mặc định của Server. */
  public static final int PORT = 5000;

  private ServerSocket server;

  /** Khởi tạo Server và bắt đầu lắng nghe kết nối. */
  public Server() {
    try {
      server = new ServerSocket(PORT);
      System.out.println("Server đang chạy tại cổng " + PORT + "...");
      initConnections();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void initConnections() {
    try {
      while (true) {
        Socket clientSocket = server.accept();
        System.out.println("Có Client mới kết nối: " + clientSocket.getInetAddress());
        /* chia luồng xử lý cho từng Client. */
        ClientHandler handler = new ClientHandler(clientSocket);
        Thread thread = new Thread(handler);
        thread.start();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /** Phương thức chính để khởi động Server. */
  static void main() {
    new Server();
  }
}
