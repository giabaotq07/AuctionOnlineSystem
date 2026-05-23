package app.server.command;

import app.common.dto.AuctionDetailResponse;
import app.common.dto.SetAutoBidRequest;
import app.common.dto.SetAutoBidResponse;
import app.common.dto.WalletUpdateResponse;
import app.common.enums.ResponseType;
import app.common.exception.ValidationException;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.AuctionQueryService;
import app.server.service.AutoBidService;
import app.server.service.result.AutoBidUpdateResult;

/** SetAutoBidCommand. */
public class SetAutoBidCommand extends SafeCommand {
  private final AutoBidService autoBidService;
  private final AuctionQueryService auctionQueryService;

  /** SetAutoBidCommand. */
  public SetAutoBidCommand(AutoBidService autoBidService, AuctionQueryService auctionQueryService) {
    this.autoBidService = autoBidService;
    this.auctionQueryService = auctionQueryService;
  }

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) {
    SetAutoBidRequest request =
        requirePayload(packet, SetAutoBidRequest.class, "Dữ liệu auto-bid không hợp lệ.");
    if (request.auctionId() <= 0) {
      throw new ValidationException("Dữ liệu auto-bid không hợp lệ.");
    }
    int auctionId = request.auctionId();
    AutoBidUpdateResult result =
        autoBidService.setAutoBid(
            auctionId, requireUser(clientHandler), request.maxAmount(), request.incrementAmount());
    SetAutoBidResponse response =
        new SetAutoBidResponse(
            result.auction().getId(),
            result.autoBid().getMaxAmount(),
            result.autoBid().getIncrementAmount(),
            result.autoBid().isEnabled(),
            result.auction().getHighestBid(),
            result.auction().getWinnerId() == null ? 0 : result.auction().getWinnerId());
    sendSuccess(clientHandler, "Cập nhật auto-bid thành công.", response);
    notifyAfterSetAutoBid(clientHandler, result);
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.SET_AUTO_BID_RESULT;
  }

  @Override
  protected String unexpectedErrorMessage() {
    return "Không thể cập nhật auto-bid.";
  }

  private void sendWalletUpdate(ClientHandler clientHandler, AutoBidUpdateResult result) {
    WalletUpdateResponse response =
        new WalletUpdateResponse(app.common.mapper.ModelMapper.toUserDto(result.user()));
    clientHandler.sendPacket(PacketRes.of(ResponseType.WALLET_UPDATED, "OK", response));
  }

  private void notifyAfterSetAutoBid(ClientHandler clientHandler, AutoBidUpdateResult result) {
    try {
      sendWalletUpdate(clientHandler, result);
    } catch (Exception e) {
      logger.warn(
          "Auto-bid for auction {} succeeded, but wallet notification failed",
          result.auction().getId(),
          e);
    }
    try {
      Server.broadcastAuctionList(auctionQueryService);
      broadcastAuctionDetail(result.auction().getId());
    } catch (Exception e) {
      logger.warn(
          "Auto-bid for auction {} succeeded, but auction notification failed",
          result.auction().getId(),
          e);
    }
  }

  private void broadcastAuctionDetail(int auctionId) {
    try {
      AuctionDetailResponse response =
          new AuctionDetailResponse(
              app.common.mapper.ModelMapper.toAuctionDto(
                  auctionQueryService.getAuctionDetail(auctionId)));
      Server.broadcastToAuctionViewers(
          auctionId, PacketRes.of(ResponseType.AUCTION_DETAIL_UPDATED, "OK", response), -1);
    } catch (Exception e) {
      logger.error("Failed to broadcast auction detail", e);
    }
  }

}
