package app.network;

import app.data.AuctionSummary;
import app.data.HistoryRequest;
import app.data.HistoryResponse;
import app.enums.PacketType;
import app.models.PacketReq;
import app.models.PacketRes;
import app.service.AuctionService;
import java.util.List;

public class FetchHistoryCommand implements Command {
  private final AuctionService auctionService;
  public FetchHistoryCommand(AuctionService auctionService) {
    this.auctionService = auctionService;
  }
  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    HistoryRequest request = packet.getData(HistoryRequest.class);
    int userId = request.userId();
    List<AuctionSummary> summaries = auctionService.getHistorySummaries(userId);
    HistoryResponse response = new HistoryResponse(true, "OK", summaries);
    clientHandler.sendMessage(PacketRes.of(PacketType.FETCH_HISTORY, response));
  }
}
