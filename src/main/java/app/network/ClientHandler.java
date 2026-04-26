package app.network;

import app.models.CommandType;
import app.models.MessagePacket;
import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.HashMap;

public class ClientHandler implements Runnable {
  private final Socket socket;
    private PrintWriter writer;
  private String username;
  private final Gson gson = new Gson();

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
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      writer = new PrintWriter(socket.getOutputStream(), true);
      String line;
      while ((line = reader.readLine()) != null) {
        MessagePacket<?> packet = gson.fromJson(line, MessagePacket.class);
        handlePacket(packet);
      }
    } catch (IOException e) {
      close();
    }
  }

  /** hàm này để đây minh hoạ, chưa có các lớp DAO để gọi */
  private void handlePacket(MessagePacket<?> packet) {
      Command command = COMMANDS.get(packet.getType());
      if (command != null) {
          command.execute(this, packet);
      } else {
          System.out.println("[SERVER] Unrecognized command type: " + packet.getType());
      }
  }

  public void sendMessage(MessagePacket<?> packet) {
    if (writer != null) writer.println(gson.toJson(packet));
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
    } catch (IOException _) {
    }
  }
}
