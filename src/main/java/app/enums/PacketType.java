package app.enums;

import java.io.Serializable;

public enum PacketType implements Serializable {
  LOGIN,
  LOGOUT,
  REGISTER,
  PLACE_BID,
  CREATE_AUCTION,
  CHAT,
  FETCH_AUCTIONS,
  FETCH_HISTORY,
  FETCH_AUCTION_DETAIL,
  FETCH_AUCTION_RESULT,
  ERROR,
  SUCCESS
}
