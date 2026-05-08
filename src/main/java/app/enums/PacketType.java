package app.enums;

import java.io.Serializable;

public enum PacketType implements Serializable {
  LOGIN,
  LOGOUT,
  REGISTER,
  PLACE_BID,
  CREATE_AUCTION,
  CHAT,
  ERROR,
  SUCCESS
}
