package app.network;

import app.dao.AuctionDAO;
import app.dao.BidDAO;
import app.dao.ItemDAO;
import app.dao.impl.MySqlAuctionDAO;
import app.dao.impl.MySqlBidDAO;
import app.dao.impl.MySqlItemDAO;
import app.data.AuctionSummary;
import app.data.HistoryRequest;
import app.data.HistoryResponse;
import app.enums.PacketType;
import app.models.PacketReq;
import app.models.PacketRes;
import app.service.AuctionService;
import java.util.List;

public class FetchHistoryCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    HistoryRequest request = packet.getData(HistoryRequest.class);
    int userId = request.userId();
    AuctionDAO auctionDAO = new MySqlAuctionDAO();
    BidDAO bidDAO = new MySqlBidDAO();
    ItemDAO itemDAO = new MySqlItemDAO();
    AuctionService auctionService = new AuctionService(auctionDAO, bidDAO, itemDAO);
    List<AuctionSummary> summaries = auctionService.getHistorySummaries(userId);

    HistoryResponse response = new HistoryResponse(true, "OK", summaries);
    clientHandler.sendMessage(PacketRes.of(PacketType.FETCH_HISTORY, response));
  }
}
