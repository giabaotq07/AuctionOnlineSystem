package app.server.command;

import app.common.dto.AuctionSummariesResponse;
import app.common.enums.ResponseType;
import app.common.protocol.PacketReq;
import app.server.network.ClientHandler;
import app.server.service.AuctionQueryService;

/** FetchAuctionSummariesCommand. */
public class FetchAuctionSummariesCommand extends SafeCommand {
  private final AuctionQueryService auctionQueryService;

  /** FetchAuctionSummariesCommand. */
  public FetchAuctionSummariesCommand(AuctionQueryService auctionQueryService) {
    this.auctionQueryService = auctionQueryService;
  }

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) {
    AuctionSummariesResponse response =
        new AuctionSummariesResponse(auctionQueryService.getAuctionPreviews());
    sendSuccess(clientHandler, "OK", response);
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.FETCH_AUCTION_SUMMARIES_RESULT;
  }

  @Override
  protected String unexpectedErrorMessage() {
    return "Không thể tải danh sách đấu giá";
  }
}
