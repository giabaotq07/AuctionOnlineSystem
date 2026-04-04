package Server.SimpleServer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;

/** Lớp xử lý luồng dữ liệu cho từng Client kết nối tới. */
public class ClientHandler implements Runnable {
  private final Socket clientSocket;
  private ObjectInputStream in;

  /** Chuỗi ký tự để dừng kết nối. */
  public static final String STOP_STRING = "STOP";

  /**
   * Khởi tạo bộ xử lý client với socket cụ thể.
   *
   * @param socket socket của client.
   */
  public ClientHandler(Socket socket) {
    this.clientSocket = socket;
  }

  @Override
  public void run() {
    try {
      in = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream()));
      readMessages();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      close();
    }
  }

  private void readMessages() {
    try {
      String line;
      while (true) {
        line = in.readUTF();
        if (line.equalsIgnoreCase(STOP_STRING)) {
          System.out.println("Client ngắt kết nối.");
          break;
        }
        if (!line.equals("\n")) {
          System.out.println("Client nói: " + line);
        }
      }
    } catch (SocketException e) {
      System.out.println("Client mất kết nối.");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void close() {
    try {
      if (in != null) {
        in.close();
      }
      if (clientSocket != null) {
        clientSocket.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
