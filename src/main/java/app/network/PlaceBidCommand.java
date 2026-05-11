package app.network;

import app.dao.AuctionDAO;
import app.dao.AutoBidDAO;
import app.dao.BidDAO;
import app.dao.ItemDAO;
import app.dao.impl.MySqlAuctionDAO;
import app.dao.impl.MySqlAutoBidDAO;
import app.dao.impl.MySqlBidDAO;
import app.dao.impl.MySqlItemDAO;
import app.data.PlaceBidRequest;
import app.data.PlaceBidResponse;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.models.Auction;
import app.models.BidTransaction;
import app.models.PacketReq;
import app.models.PacketRes;
import app.service.AuctionService;
import app.service.BidService;
import app.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaceBidCommand implements Command {
  private Logger logger = LoggerFactory.getLogger(PlaceBidCommand.class);
  private final AuctionService auctionService;
  private final BidService bidService;
  private final ItemService itemService;

  public PlaceBidCommand() {
    AuctionDAO auctionDAO = new MySqlAuctionDAO();
    BidDAO bidDAO = new MySqlBidDAO();
    AutoBidDAO autoBidDAO = new MySqlAutoBidDAO();
    ItemDAO itemDAO = new MySqlItemDAO();

    this.auctionService = new AuctionService(auctionDAO, bidDAO);
    this.bidService = new BidService(bidDAO, autoBidDAO, auctionDAO);
    this.itemService = new ItemService(itemDAO);
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    PlaceBidRequest placeBidRequest = packet.getData(PlaceBidRequest.class);
    int sessionId = placeBidRequest.sessionId();
    Auction session = auctionService.getAuctionById(sessionId);
    long bidAmount = placeBidRequest.bidAmount();
    long currentPrice = placeBidRequest.currentPrice();
    int bidderId = placeBidRequest.bidderId();
    try {
      if (bidAmount <= 0) {
        throw new NumberFormatException();
      }
      if (bidAmount <= currentPrice) {
        logger.error("Lỗi trả giá", "Giá đặt phải cao hơn giá hiện tại!");
        return;
      }
    } catch (NumberFormatException e) {
      logger.error("Lỗi", "Giá nhập phải là số nguyên dương");
      return;
    }

    // KIỂM TRA SƠ BỘ: Nếu giá nhập thấp hơn giá đang hiển thị thì chặn luôn ở Client cho nhanh
    try {
      // Gọi BidService để lưu vào MySQL
      bidService.placeBid(session.getId(), bidderId, bidAmount);
    } catch (ServiceException e) {
      logger.error("Lỗi trả giá", "Giá đặt phải cao hơn giá hiện tại!");
    } catch (Exception e) {
      logger.error("Lỗi đặt giá", e.getMessage());
    }
    BidTransaction highestBidTransaction;
    bidderId = 0;
    long amount = itemService.getById(session.getItemId()).orElse(null).getStartingPrice();
    String itemName = itemService.getById(session.getItemId()).orElse(null).getName();
    String bidderName = "";
    if (bidService.getHighestBid(session.getId()).isPresent()) {
      highestBidTransaction = bidService.getHighestBid(session.getId()).get();
      bidderId = highestBidTransaction.getBidderId();
      amount = highestBidTransaction.getAmount();
      bidderName = highestBidTransaction.getBidderName();
    }
    PlaceBidResponse response = new PlaceBidResponse(bidderId, amount, itemName, bidderName);
    PacketRes packetResponse = PacketRes.of(PacketType.PLACE_BID, response);
    clientHandler.sendMessage(packetResponse);
    Server.broadcast(packetResponse, clientHandler.getUser().getId());
  }
}
