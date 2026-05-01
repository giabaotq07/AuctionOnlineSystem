package app.network;

import app.models.Auction;
import app.models.BidTransaction;
import app.models.MessagePacket;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class Client {
  private static volatile Client instance;
  private Socket socket;
  private ObjectOutputStream out;
  private ObjectInputStream in;
  private Consumer<MessagePacket<?>> onMessageReceived;
  private boolean isConnected = false;

  public static Client getInstance() {
    if (instance == null) {
      synchronized (Client.class) {
        if (instance == null) instance = new Client();
      }
    }
    return instance;
  }

  public void connect() throws IOException {
    System.out.println("[CLIENT] Đang kết nối...");
    socket = new Socket("127.0.0.1", 5000);
    System.out.println("[CLIENT] Kết nối thành công!");
    out = new ObjectOutputStream(socket.getOutputStream());
    out.flush();
    in = new ObjectInputStream(socket.getInputStream());
    Thread thread = new Thread(this::listen);
    thread.setDaemon(true);
    thread.start();
    isConnected = true;
  }

  private void listen() {
    try {
      MessagePacket<?> packet;
      while ((packet = (MessagePacket<?>) in.readObject()) != null) {
        System.out.println("[Server] " + packet.getType());
        switch (packet.getType()) {
          case LOGIN:
            onMessageReceived.accept(packet);
            break;
          case CREATE_AUCTION:
            Auction session = (Auction) packet.getData();
            boolean exists =
                app.models.DataStore.sessions.stream().anyMatch(s -> s.getId() == session.getId());
            if (!exists) {
              app.models.DataStore.sessions.add(session);
              app.models.AuctionStateManager.getInstance().addSession(session);
            }
            break;

          case PLACE_BID:
            Auction updatedSession = (Auction) packet.getData();
            app.models.DataStore.sessions.stream()
                .filter(s -> s.getId() == updatedSession.getId())
                .findFirst()
                .ifPresent(
                    s -> {
                      s.getBidHistory().clear();
                      s.getBidHistory().addAll(updatedSession.getBidHistory());
                      s.setStatus(updatedSession.getStatus());

                      if (!s.getBidHistory().isEmpty()) {
                        BidTransaction lastBidTransaction =
                            s.getBidHistory().get(s.getBidHistory().size() - 1);
                        s.notifyObserversNewBid(
                            lastBidTransaction.getAmount(),
                            lastBidTransaction.getBidder().getName());
                      }
                    });
            break;
        }

        if (onMessageReceived != null) {
          // Đẩy về cho Controller xử lýw
          onMessageReceived.accept(packet);
        }
      }
    } catch (IOException | ClassNotFoundException e) {
      System.err.println("Mất kết nối Server.");
      isConnected = false;
    }
  }

  public boolean isConnected() {
    return isConnected;
  }

  public void sendRequest(MessagePacket<?> packet) {
    if (out != null) {
      try {
        out.reset(); // Reset Java object stream cache
        out.writeObject(packet);
        out.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void setOnMessageReceived(Consumer<MessagePacket<?>> handler) {
    this.onMessageReceived = handler;
  }
}
