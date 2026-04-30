package app.network;

import app.models.CommandType;
import app.models.MessagePacket;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {
  private final Socket socket;
  private ObjectOutputStream writer;
  private String username;

  // Lui lenh thanh 1 map
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
      command.execute(this, packet); // cái này đc kế thừa và cài đặt ở lớp con
    } else {
      System.out.println("[SERVER] Unrecognized command type: " + packet.getType());
    }

    switch (packet.getType()) {
      case CHAT:
        String content = (String) packet.getData();
        MessagePacket<String> chatPacket = new MessagePacket<>(CommandType.CHAT, content);
        chatPacket.setMessage(this.username);
        Server.broadcast(chatPacket);
        break;

      case CREATE_AUCTION:
        // Broadcast the new auction session to everyone else
        MessagePacket<app.models.AuctionSession> auctionPacket =
            new MessagePacket<>(
                CommandType.CREATE_AUCTION, (app.models.AuctionSession) packet.getData());
        auctionPacket.setMessage(this.username);
        Server.broadcast(auctionPacket);
        break;

      case PLACE_BID:
        // broadcast bid session to client
        MessagePacket<app.models.AuctionSession> placeBidPacket =
            new MessagePacket<>(
                CommandType.PLACE_BID, (app.models.AuctionSession) packet.getData());
        placeBidPacket.setMessage(this.username);
        Server.broadcast(placeBidPacket);
        break;
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
