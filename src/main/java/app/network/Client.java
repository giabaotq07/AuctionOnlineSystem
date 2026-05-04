package app.network;

import app.dao.AuctionDAO;
import app.dao.AutoBidDAO;
import app.dao.BidDAO;
import app.dto.BidRequest;
import app.models.*;
import app.service.BidObserverService;
import app.service.BidService;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class Client {
  private static volatile Client instance;
  private Socket socket;
  private String username;
  private ObjectOutputStream out;
  private ObjectInputStream in;
  private Consumer<Packet> onMessageReceived;
  private boolean isConnected = false;

  private Client() {}

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
      Packet packet;
      while ((packet = (Packet) in.readObject()) != null) {
        System.out.println("[Server] " + packet.getType());
        // xử lý lại vấn đề thông báo cho controller
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
              AuctionStateManager.getInstance().addSession(session);
            }
            break;

          case PLACE_BID:
            BidRequest bidRequest = (BidRequest) packet.getData();
            AuctionDAO auctionDAO = new AuctionDAO();
            BidDAO bidDAO = new BidDAO();
            AutoBidDAO autoBidDAO = new AutoBidDAO();
            BidObserverService observerService = new BidObserverService();
            BidService bidService = new BidService(bidDAO, autoBidDAO, auctionDAO, observerService);
            bidService.placeBid(
                bidRequest.sessionId(),
                bidRequest.bidTransaction().getBidderId(),
                bidRequest.bidTransaction().getAmount());
            break;

          case CHAT:
            onMessageReceived.accept(packet);
            break;
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

  public void sendRequest(Packet packet) {
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

  public void setOnMessageReceived(Consumer<Packet> handler) {
    this.onMessageReceived = handler;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }
}
