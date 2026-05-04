package app.network;

import app.enums.PacketType;
import app.models.Packet;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {
  private final Socket socket;
  private ObjectOutputStream writer;
  private String username;

  private static final Map<PacketType, Command> COMMANDS = new HashMap<>();

  static {
    COMMANDS.put(PacketType.LOGIN, new LoginCommand());
    COMMANDS.put(PacketType.CHAT, new ChatCommand());
    COMMANDS.put(PacketType.PLACE_BID, new PlaceBidCommand());
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
      Packet packet;
      while ((packet = (Packet) reader.readObject()) != null) {
        handlePacket(packet);
      }
    } catch (IOException | ClassNotFoundException e) {
      close();
    }
  }

  private void handlePacket(Packet packet) {
    Command command = COMMANDS.get(packet.getType());
    if (command != null) {
      command.execute(this, packet);
    } else {
      System.out.println("[SERVER] Unrecognized command type: " + packet.getType());
    }
  }

  public void sendMessage(Packet packet) {
    if (writer != null) {
      try {
        writer.reset();
        writer.writeObject(packet);
        writer.flush();
      } catch (IOException e) {
        System.err.println(
            "[SERVER] Failed to send message to " + username + ": " + e.getMessage());
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
