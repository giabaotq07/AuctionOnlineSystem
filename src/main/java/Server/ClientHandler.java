package Server;

import app.models.core.Message;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/** Lớp xử lý luồng dữ liệu cho từng Client kết nối tới. */
public class ClientHandler implements Runnable {
  private final Socket clientSocket;
  private ObjectInputStream input;
  private ObjectOutputStream output;
  private final String handlerId;

  public ClientHandler(Socket socket, String handlerId) {
    this.clientSocket = socket;
    this.handlerId = handlerId;
  }

  @Override
  public void run() {
    try {
      output = new ObjectOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
      output.flush();
      input = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream()));
      Message message;
      while ((message = (Message) input.readObject()) != null) {
        if (message.message().equals(Server.STOP_STRING)) break;
        System.out.println(message);
        Server.broadcast(message);
      }
    } catch (EOFException | SocketException e) {
      // Client mất kết nối.
    } catch (Exception e) {
      System.err.println("Lỗi xử lý Client " + handlerId + ": " + e.getMessage());
    } finally {
      close();
    }
  }

  public synchronized void sendMessage(Message message) {
    try {
      if (output != null) {
        output.writeObject(message);
        output.flush();
      }
    } catch (IOException e) {
      Server.removeClient(handlerId);
    }
  }

  private void close() {
    Server.removeClient(handlerId);
    try {
      if (input != null) input.close();
      if (output != null) output.close();
      if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
    } catch (IOException e) {
      // Ignore
    }
  }
}
