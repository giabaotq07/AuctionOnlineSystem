package Server;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

/** Lớp Client để kết nối và gửi dữ liệu tới Server. */
public class Client {
  private Socket socket;
  private ObjectOutputStream out;
  private Scanner clientIn;

  /** Khởi tạo kết nối tới server. */
  public Client() {
    try {
      socket = new Socket("127.0.0.1", Server.PORT);
      System.out.println("Đã kết nối tới Server tại " + socket.getInetAddress());
      out = new ObjectOutputStream(socket.getOutputStream());
      clientIn = new Scanner(System.in);
      writeMessages();
      // writeObject();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      close();
    }
  }

  private void writeObject() {}

  private void writeMessages() {
    String line = "";
    try {
      while (!line.equals(ClientHandler.STOP_STRING)) {
        line = clientIn.nextLine();
        if (!line.trim().isEmpty()) {
          out.writeUTF(line);
        }
        out.flush();
      }
    } catch (SocketException e) {
      System.err.println("Server mất kết nối.");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void close() {
    try {
      if (out != null) {
        out.close();
      }
      if (clientIn != null) {
        clientIn.close();
      }
      if (socket != null) {
        socket.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /** Phương thức chính để chạy Client. */
  static void main() {
    new Client();
  }
}
