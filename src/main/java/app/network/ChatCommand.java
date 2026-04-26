package app.network;

import app.models.CommandType;
import app.models.MessagePacket;

public class ChatCommand implements Command {
    @Override
    public void execute(ClientHandler clientHandler, MessagePacket<?> packet) {
        String content = (String) packet.getData();
        MessagePacket<String> chatPacket = new MessagePacket<>(CommandType.CHAT, content);
        chatPacket.setMessage(clientHandler.getUsername());
        Server.broadcast(chatPacket);
    }
}

