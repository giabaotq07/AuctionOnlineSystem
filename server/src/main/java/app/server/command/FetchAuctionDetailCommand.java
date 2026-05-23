package app.server.command;

import app.common.dto.AuctionDetailRequest;
import app.common.dto.AuctionDetailResponse;
import app.common.enums.ResponseType;
import app.common.exception.ValidationException;
import app.common.protocol.PacketReq;
import app.server.network.ClientHandler;
import app.server.service.AuctionQueryService;

/** FetchAuctionDetailCommand. */
public class FetchAuctionDetailCommand extends SafeCommand {
  private final AuctionQueryService auctionQueryService;

  /** FetchAuctionDetailCommand. */
  public FetchAuctionDetailCommand(AuctionQueryService auctionQueryService) {
    this.auctionQueryService = auctionQueryService;
  }

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) {
    AuctionDetailRequest request =
        requirePayload(packet, AuctionDetailRequest.class, "Dữ liệu phiên đấu giá không hợp lệ.");
    if (request.auctionId() <= 0) {
      throw new ValidationException("Phiên đấu giá không hợp lệ.");
    }
    clientHandler.getSession().setViewingAuctionId(request.auctionId());
    if (auctionQueryService.isAuctionVersionCurrent(request.auctionId(), request.knownVersion())) {
      AuctionDetailResponse response =
          AuctionDetailResponse.notModified(request.auctionId(), request.knownVersion());
      sendSuccess(clientHandler, "OK", response);
      return;
    }
    AuctionDetailResponse response =
        new AuctionDetailResponse(
            app.common.mapper.ModelMapper.toAuctionDto(
                auctionQueryService.getAuctionDetail(request.auctionId())));
    sendSuccess(clientHandler, "OK", response);
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.AUCTION_RESULT;
  }

  @Override
  protected String unexpectedErrorMessage() {
    return "Không thể tải chi tiết đấu giá.";
  }
}
