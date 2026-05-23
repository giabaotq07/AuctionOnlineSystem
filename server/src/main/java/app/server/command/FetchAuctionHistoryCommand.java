package app.server.command;

import app.common.dto.AuctionHistoryRequest;
import app.common.dto.AuctionHistoryResponse;
import app.common.enums.ResponseType;
import app.common.protocol.PacketReq;
import app.server.network.ClientHandler;
import app.server.service.AuctionQueryService;

/** FetchAuctionHistoryCommand. */
public class FetchAuctionHistoryCommand extends SafeCommand {
  private final AuctionQueryService auctionQueryService;

  /** FetchAuctionHistoryCommand. */
  public FetchAuctionHistoryCommand(AuctionQueryService auctionQueryService) {
    this.auctionQueryService = auctionQueryService;
  }

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) {
    int userId = requireUser(clientHandler).getId();
    AuctionHistoryRequest request = packet == null ? null : packet.getData(AuctionHistoryRequest.class);
    int sinceVersion = request == null ? -1 : request.sinceVersion();
    var auctions = auctionQueryService.getHistoryAuctionPreviews(userId);
    boolean fullSnapshot = sinceVersion < 0;
    if (!fullSnapshot) {
      auctions = auctions.stream().filter(auction -> auction.version() > sinceVersion).toList();
    }
    AuctionHistoryResponse response = new AuctionHistoryResponse(auctions, fullSnapshot);
    sendSuccess(clientHandler, "OK", response);
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.FETCH_AUCTION_HISTORY_RESULT;
  }

  @Override
  protected String unexpectedErrorMessage() {
    return "Không thể tải lịch sử đấu giá";
  }
}
