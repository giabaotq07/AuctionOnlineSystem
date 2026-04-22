package app.models;

public enum CommandType {
  // Auth & User
  LOGIN,
  LOGOUT,
  REGISTER,

  // Auction Actions
  PLACE_BID, // Client đặt giá
  CREATE_AUCTION, // Seller tạo phiên
  CHAT,
  //status
  OPEN,
  RUNNING,
  FINISHED,
  PAID,
  CANCELED,

  // Server Push (Realtime)
  UPDATE_PRICE, // Notify giá mới cho tất cả Client
  AUCTION_FINISHED, // Thông báo kết thúc phiên

  // System
  ERROR, // Thông báo lỗi (sai giá, mất kết nối) [cite: 57, 60]
  SUCCESS
}
