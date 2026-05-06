package app.network;

import app.dao.AuctionDAO;
import app.dao.AutoBidDAO;
import app.dao.BidDAO;
import app.dao.impl.MySqlAuctionDAO;
import app.dao.impl.MySqlAutoBidDAO;
import app.dao.impl.MySqlBidDAO;
import app.dto.BidRequest;
import app.models.*;
import app.service.BidObserverService;
import app.service.BidService;
import app.utils.JsonUtil;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class Client {
  private static volatile Client instance;
  private Socket socket;
  private String username;
  private BufferedWriter writer;
  private BufferedReader reader;
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
    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    writer.flush();
    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    Thread thread = new Thread(this::listen);
    thread.setDaemon(true);
    thread.start();
    isConnected = true;
  }

  private void listen() {
    try {
      String json;
      while ((json = reader.readLine()) != null) {
        Packet packet = JsonUtil.fromJson(json, Packet.class);
        System.out.println("[Server] " + packet.getType());
        // xử lý lại vấn đề thông báo cho controller
        switch (packet.getType()) {
          case LOGIN, CHAT:
            onMessageReceived.accept(packet);
            System.out.println("[CLIENT] Nhận được tin nhắn: " + packet.getData());
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
            AuctionDAO auctionDAO = new MySqlAuctionDAO();
            BidDAO bidDAO = new MySqlBidDAO();
            AutoBidDAO autoBidDAO = new MySqlAutoBidDAO();
            BidObserverService observerService = new BidObserverService();
            BidService bidService = new BidService(bidDAO, autoBidDAO, auctionDAO, observerService);
            bidService.placeBid(
                bidRequest.sessionId(),
                bidRequest.bidTransaction().getBidderId(),
                bidRequest.bidTransaction().getAmount());
            break;
        }
      }
    } catch (IOException e) {
      System.err.println("Mất kết nối Server.");
      isConnected = false;
    }
  }

  public boolean isConnected() {
    return isConnected;
  }

  public void sendRequest(Packet packet) {
    if (writer != null) {
      try {
        String json = JsonUtil.toJson(packet);
        writer.write(json);
        writer.newLine();
        writer.flush();
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
