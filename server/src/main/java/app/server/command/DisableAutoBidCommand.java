package app.server.command;

import app.common.dto.DisableAutoBidRequest;
import app.common.dto.DisableAutoBidResponse;
import app.common.dto.WalletUpdateResponse;
import app.common.enums.ResponseType;
import app.common.exception.ServiceException;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.service.AutoBidService;
import app.server.service.result.AutoBidUpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** DisableAutoBidCommand. */
public class DisableAutoBidCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(DisableAutoBidCommand.class);
  private final AutoBidService autoBidService;

  /** DisableAutoBidCommand. */
  public DisableAutoBidCommand(AutoBidService autoBidService) {
    this.autoBidService = autoBidService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      DisableAutoBidRequest request = packet.getData(DisableAutoBidRequest.class);
      if (request == null || request.auctionId() <= 0) {
        sendError(clientHandler, "Dữ liệu auto-bid không hợp lệ.");
        return;
      }
      AutoBidUpdateResult result =
          autoBidService.disableAutoBid(request.auctionId(), clientHandler.getUser());
      DisableAutoBidResponse response =
          new DisableAutoBidResponse(result.auction().getId(), result.autoBid().isEnabled());
      clientHandler.sendPacket(
          PacketRes.of(ResponseType.DISABLE_AUTO_BID_RESULT, "Đã tắt auto-bid.", response));
      sendWalletUpdate(clientHandler, result);
    } catch (ServiceException e) {
      logger.warn("Disable auto-bid failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected disable auto-bid error", e);
      sendError(clientHandler, "Không thể tắt auto-bid.");
    }
  }

  private void sendWalletUpdate(ClientHandler clientHandler, AutoBidUpdateResult result) {
    WalletUpdateResponse response =
        new WalletUpdateResponse(app.common.mapper.ModelMapper.toUserDto(result.user()));
    clientHandler.sendPacket(PacketRes.of(ResponseType.WALLET_UPDATED, "OK", response));
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(ResponseType.DISABLE_AUTO_BID_RESULT, message));
  }
}
