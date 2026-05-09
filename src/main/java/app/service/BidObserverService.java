package app.service;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class BidObserverService {
  public BidObserverService() {}

  // Map: sessionId → danh sách socket của các client đang xem
  private final Map<Integer, List<PrintWriter>> subscribers = new ConcurrentHashMap<>();

  // Client mở màn hình phiên → đăng ký vào đây
  public void subscribe(int sessionId, PrintWriter clientSocket) {
    subscribers.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(clientSocket);
  }

  // Client đóng tab → huỷ đăng ký
  public void unsubscribe(int sessionId, PrintWriter clientSocket) {
    subscribers.getOrDefault(sessionId, List.of()).remove(clientSocket);
  }

  // BidService gọi sau mỗi bid thành công
  /*
  public void notifyBidUpdated(int sessionId) {
    List<PrintWriter> clients = subscribers.getOrDefault(sessionId, List.of());

    String message = "{\"event\":\"BID_UPDATED\", \"sessionId\":" + sessionId + "}";

    for (PrintWriter client : clients) {
      client.println(message); // đẩy xuống từng client qua socket
    }
  }
   */
}
