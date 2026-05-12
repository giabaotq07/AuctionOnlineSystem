package app.network;

import app.data.AuctionSummary;
import app.data.AuctionsResponse;
import app.enums.PacketType;
import app.models.PacketReq;
import app.models.PacketRes;
import app.service.AuctionService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FetchAuctionsCommand implements Command {
  Logger logger = LoggerFactory.getLogger(FetchAuctionsCommand.class);
  private final AuctionService auctionService;

  public FetchAuctionsCommand(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    List<AuctionSummary> summaries = auctionService.getAuctionSummaries();
    AuctionsResponse response = new AuctionsResponse(true, "OK", summaries);
    clientHandler.sendMessage(PacketRes.of(PacketType.FETCH_AUCTIONS, response));
  }
}
