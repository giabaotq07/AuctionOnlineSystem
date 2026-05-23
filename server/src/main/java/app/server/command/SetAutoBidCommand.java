package app.server.command;

import app.common.dto.AuctionDetailResponse;
import app.common.dto.SetAutoBidRequest;
import app.common.dto.SetAutoBidResponse;
import app.common.dto.WalletUpdateResponse;
import app.common.enums.ResponseType;
import app.common.exception.DatabaseException;
import app.common.exception.ServiceException;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.AuctionQueryService;
import app.server.service.AutoBidService;
import app.server.service.result.AutoBidUpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SetAutoBidCommand. */
public class SetAutoBidCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(SetAutoBidCommand.class);
  private final AutoBidService autoBidService;
  private final AuctionQueryService auctionQueryService;

  /** SetAutoBidCommand. */
  public SetAutoBidCommand(AutoBidService autoBidService, AuctionQueryService auctionQueryService) {
    this.autoBidService = autoBidService;
    this.auctionQueryService = auctionQueryService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    int auctionId = 0;
    try {
      SetAutoBidRequest request = packet.getData(SetAutoBidRequest.class);
      if (request == null || request.auctionId() <= 0) {
        sendError(clientHandler, "Dữ liệu auto-bid không hợp lệ.");
        return;
      }
      auctionId = request.auctionId();
      AutoBidUpdateResult result =
          autoBidService.setAutoBid(
              auctionId, clientHandler.getUser(), request.maxAmount(), request.incrementAmount());
      SetAutoBidResponse response =
          new SetAutoBidResponse(
              result.auction().getId(),
              result.autoBid().getMaxAmount(),
              result.autoBid().getIncrementAmount(),
              result.autoBid().isEnabled(),
              result.auction().getHighestBid(),
              result.auction().getWinnerId() == null ? 0 : result.auction().getWinnerId());
      clientHandler.sendPacket(
          PacketRes.of(
              ResponseType.SET_AUTO_BID_RESULT, "Cập nhật auto-bid thành công.", response));
      sendWalletUpdate(clientHandler, result);
      Server.broadcastAuctionList(auctionQueryService);
      broadcastAuctionDetail(result.auction().getId());
    } catch (ServiceException e) {
      logger.warn("Set auto-bid failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (DatabaseException e) {
      logger.error("Set auto-bid database error", e);
      sendError(clientHandler, "Lỗi dữ liệu hoặc kết nối, vui lòng thử lại.");
    } catch (Exception e) {
      logger.error("Unexpected set auto-bid error", e);
      sendError(clientHandler, "Không thể cập nhật auto-bid.");
    }
  }

  private void sendWalletUpdate(ClientHandler clientHandler, AutoBidUpdateResult result) {
    WalletUpdateResponse response =
        new WalletUpdateResponse(app.common.mapper.ModelMapper.toUserDto(result.user()));
    clientHandler.sendPacket(PacketRes.of(ResponseType.WALLET_UPDATED, "OK", response));
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

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(ResponseType.SET_AUTO_BID_RESULT, message));
  }
}
