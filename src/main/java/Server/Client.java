package Server;

import Common.core.Message;
import Common.protocol.MessageHandler;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class Client {
  private final Socket socket;
  private final ObjectOutputStream output;
  private final ObjectInputStream input;
  private final String clientIp;
  private static volatile Client instance;
  private boolean isClosing = false;
  private boolean isListening = false;
  private MessageHandler<Message> messageHandler;
  private Thread listenThread;

  private Client() throws IOException {
    socket = new Socket("127.0.0.1", Server.PORT);
    output = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    output.flush();
    input = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
    clientIp = (socket.getInetAddress()) + ":" + (socket.getLocalPort());
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

  public void setMessageHandler(MessageHandler<Message> handler) {
    this.messageHandler = handler;
  }

  public void removeMessageHandler() {
    this.messageHandler = null;
  }

  public void sendMessages(String line) throws IOException {
    if (!line.trim().isEmpty()) {
      Message message = new Message(this.clientIp, line);
      output.writeObject(message);
      output.flush();
      System.out.println(message);
    }
  }

  public void receiveMessage() {
    if (listenThread != null && listenThread.isAlive()) return;
    listenThread =
        new Thread(
            () -> {
              try {
                while (!isClosing) {
                  Message message = (Message) input.readObject();
                  if (message != null && messageHandler != null) {
                    // System.out.println("\n[SERVER]: " + data);
                    messageHandler.messageHandlerReceiver(message);
                    // System.out.println(message);
                  }
                }
              } catch (EOFException | SocketException e) {
                if (!isClosing) System.err.println("\n[LỖI] Mất kết nối tới Server!");
              } catch (Exception e) {
                e.printStackTrace();
              } finally {
                close();
              }
            });
    listenThread.setDaemon(true);
    listenThread.start();
  }

  public synchronized void close() {
    if (isClosing) return;
    isClosing = true;
    try {
      if (socket != null) socket.close();
      System.out.println("Đã đóng kết nối.");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public String getClientIp() {
    return clientIp;
  }
}
