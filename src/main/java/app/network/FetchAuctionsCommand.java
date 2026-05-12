package app.network;

import app.dao.AuctionDAO;
import app.dao.BidDAO;
import app.dao.ItemDAO;
import app.dao.impl.MySqlAuctionDAO;
import app.dao.impl.MySqlBidDAO;
import app.dao.impl.MySqlItemDAO;
import app.data.AuctionSummary;
import app.data.AuctionsResponse;
import app.enums.PacketType;
import app.models.PacketReq;
import app.models.PacketRes;
import app.service.AuctionService;
import app.utils.JsonUtil;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FetchAuctionsCommand implements Command {
  Logger logger = LoggerFactory.getLogger(FetchAuctionsCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
//    logger.info("In FetchAuctionsCommand");
    //    packet.getData(AuctionsRequest.class);
    AuctionDAO auctionDAO = new MySqlAuctionDAO();
    BidDAO bidDAO = new MySqlBidDAO();
    ItemDAO itemDAO = new MySqlItemDAO();
    AuctionService auctionService = new AuctionService(auctionDAO, bidDAO, itemDAO);
    List<AuctionSummary> summaries = auctionService.getAuctionSummaries();
//    logger.info(JsonUtil.toJson(summaries));
    AuctionsResponse response = new AuctionsResponse(true, "OK", summaries);
    clientHandler.sendMessage(PacketRes.of(PacketType.FETCH_AUCTIONS, response));
  }
}
