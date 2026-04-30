package app.network;

import app.enums.CommandType;
import app.models.MessagePacket;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {
  private final Socket socket;
  private ObjectOutputStream writer;
  private String username;

  private static final Map<CommandType, Command> COMMANDS = new HashMap<>();

  static {
    COMMANDS.put(CommandType.LOGIN, new LoginCommand());
    COMMANDS.put(CommandType.CHAT, new ChatCommand());
    COMMANDS.put(CommandType.PLACE_BID, new PlaceBidCommand());
  }

  public ClientHandler(Socket socket) {
    this.socket = socket;
  }

  @Override
  public void run() {
    try {
      writer = new ObjectOutputStream(socket.getOutputStream());
      writer.flush();
      ObjectInputStream reader = new ObjectInputStream(socket.getInputStream());
      MessagePacket<?> packet;
      while ((packet = (MessagePacket<?>) reader.readObject()) != null) {
        handlePacket(packet);
      }
    } catch (IOException | ClassNotFoundException e) {
      close();
    }
  }

  private void handlePacket(MessagePacket<?> packet) {
    Command command = COMMANDS.get(packet.getType());
    if (command != null) {
      command.execute(this, packet);
    } else {
      System.out.println("[SERVER] Unrecognized command type: " + packet.getType());
    }
  }

  public void sendMessage(MessagePacket<?> packet) {
    if (writer != null) {
      try {
        writer.reset(); // Reset Java object stream cache
        writer.writeObject(packet);
        writer.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  private void close() {
    if (this.username != null) {
      Server.removeClient(this.username);
    }
    try {
      socket.close();
    } catch (IOException ignored) {
    }
  }
}
