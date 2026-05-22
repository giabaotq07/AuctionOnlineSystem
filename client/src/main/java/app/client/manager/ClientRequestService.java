package app.client.manager;

import app.client.Client;
import app.common.dto.AuctionDetailRequest;
import app.common.dto.AuctionResultRequest;
import app.common.dto.ChatRequest;
import app.common.dto.CreateAuctionRequest;
import app.common.dto.DepositRequest;
import app.common.dto.LoginRequest;
import app.common.dto.PlaceBidRequest;
import app.common.dto.RegisterRequest;
import app.common.dto.Request;
import app.common.dto.SettleWalletRequest;
import app.common.dto.UpdateAuctionRequest;
import app.common.enums.RequestType;
import app.common.protocol.PacketReq;
import java.io.IOException;
import java.math.BigDecimal;

/** Facade for client requests. */
public final class ClientRequestService {
  private static volatile ClientRequestService instance;

  private final Client client = Client.getInstance();

  private ClientRequestService() {}

  /** getInstance. */
  public static ClientRequestService getInstance() {
    if (instance == null) {
      synchronized (ClientRequestService.class) {
        if (instance == null) {
          instance = new ClientRequestService();
        }
      }
    }
    return instance;
  }

  /** isConnected. */
  public boolean isConnected() {
    return client.isConnected();
  }

  /** login. */
  public void login(LoginRequest request) throws IOException {
    send(RequestType.LOGIN, request);
  }

  /** register. */
  public void register(RegisterRequest request) throws IOException {
    send(RequestType.REGISTER, request);
  }

  /** createAuction. */
  public void createAuction(CreateAuctionRequest request) throws IOException {
    send(RequestType.CREATE_AUCTION, request);
  }

  /** updateAuction. */
  public void updateAuction(UpdateAuctionRequest request) throws IOException {
    send(RequestType.UPDATE_AUCTION, request);
  }

  /** fetchAuctionSummaries. */
  public void fetchAuctionSummaries() throws IOException {
    send(RequestType.FETCH_AUCTION_SUMMARIES, null);
  }

  /** fetchAuctionHistory. */
  public void fetchAuctionHistory(int sinceVersion) throws IOException {
    send(RequestType.FETCH_AUCTION_HISTORY, new app.common.dto.AuctionHistoryRequest(sinceVersion));
  }

  /** fetchAuctionDetail. */
  public void fetchAuctionDetail(int auctionId, int knownVersion) throws IOException {
    send(RequestType.FETCH_AUCTION_DETAIL, new AuctionDetailRequest(auctionId, knownVersion));
  }

  /** unwatchAuction. */
  public void unwatchAuction() throws IOException {
    send(RequestType.UNWATCH_AUCTION, null);
  }

  /** fetchAuctionResult. */
  public void fetchAuctionResult(int auctionId) throws IOException {
    send(RequestType.FETCH_AUCTION_RESULT, new AuctionResultRequest(auctionId));
  }

  /** placeBid. */
  public void placeBid(int auctionId, long bidAmount) throws IOException {
    send(RequestType.PLACE_BID, new PlaceBidRequest(auctionId, bidAmount));
  }

  /** deposit. */
  public void deposit(BigDecimal amount) throws IOException {
    send(RequestType.DEPOSIT, new DepositRequest(amount));
  }

  /** settleWallet. */
  public void settleWallet(int auctionId) throws IOException {
    send(RequestType.SETTLE_WALLET, new SettleWalletRequest(auctionId));
  }

  /** chat. */
  public void chat(ChatRequest request) throws IOException {
    send(RequestType.CHAT, request);
  }

  private void send(RequestType type, Request payload) throws IOException {
    client.sendRequest(PacketReq.of(type, payload));
  }
}
