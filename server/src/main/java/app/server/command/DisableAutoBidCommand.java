package app.server.command;

import app.common.dto.DisableAutoBidRequest;
import app.common.dto.DisableAutoBidResponse;
import app.common.dto.WalletUpdateResponse;
import app.common.enums.ResponseType;
import app.common.exception.ValidationException;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.service.AutoBidService;
import app.server.service.result.AutoBidUpdateResult;

/** DisableAutoBidCommand. */
public class DisableAutoBidCommand extends SafeCommand {
  private final AutoBidService autoBidService;

  /** DisableAutoBidCommand. */
  public DisableAutoBidCommand(AutoBidService autoBidService) {
    this.autoBidService = autoBidService;
  }

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) {
    DisableAutoBidRequest request =
        requirePayload(packet, DisableAutoBidRequest.class, "Dữ liệu auto-bid không hợp lệ.");
    if (request.auctionId() <= 0) {
      throw new ValidationException("Dữ liệu auto-bid không hợp lệ.");
    }
    AutoBidUpdateResult result =
        autoBidService.disableAutoBid(request.auctionId(), requireUser(clientHandler));
    DisableAutoBidResponse response =
        new DisableAutoBidResponse(result.auction().getId(), result.autoBid().isEnabled());
    sendSuccess(clientHandler, "Đã tắt auto-bid.", response);
    notifyAfterDisableAutoBid(clientHandler, result);
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.DISABLE_AUTO_BID_RESULT;
  }

  @Override
  protected String unexpectedErrorMessage() {
    return "Không thể tắt auto-bid.";
  }

  private void sendWalletUpdate(ClientHandler clientHandler, AutoBidUpdateResult result) {
    WalletUpdateResponse response =
        new WalletUpdateResponse(app.common.mapper.ModelMapper.toUserDto(result.user()));
    clientHandler.sendPacket(PacketRes.of(ResponseType.WALLET_UPDATED, "OK", response));
  }

  private void notifyAfterDisableAutoBid(ClientHandler clientHandler, AutoBidUpdateResult result) {
    try {
      sendWalletUpdate(clientHandler, result);
    } catch (Exception e) {
      logger.warn(
          "Auto-bid disable for auction {} succeeded, but wallet notification failed",
          result.auction().getId(),
          e);
    }
  }
}
