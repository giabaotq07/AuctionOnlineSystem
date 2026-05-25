package app.client.manager;

import app.client.Client;
import app.common.dto.*;
import app.common.enums.RequestType;
import app.common.protocol.PacketReq;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Facade Pattern.
 *
 * <p>Provides a simple request API for JavaFX controllers and hides packet creation, request type
 * mapping, and socket sending details.
 */
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

  public void banUser(int userId, boolean ban) throws IOException {
    send(ban ? RequestType.BAN_USER : RequestType.UNBAN_USER, new BanUserRequest(userId, ban));
  }

  /** unwatchAuction. */
  public void unwatchAuction() throws IOException {
    send(RequestType.UNWATCH_AUCTION, null);
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

  public void cancelAuction(int auctionId, int expectedVersion) throws IOException {
    send(RequestType.CANCEL_AUCTION, new CancelAuctionRequest(auctionId, expectedVersion));
  }

  /** chat. */
  public void chat(ChatRequest request) throws IOException {
    send(RequestType.CHAT, request);
  }

  public void fetchItemImage(int itemId) throws IOException {
    send(RequestType.FETCH_ITEM_IMAGE, new FetchItemImageRequest(itemId));
  }

  public void uploadImage(int itemId, java.io.File imageFile) throws IOException {
    byte[] fileBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());
    String base64Data = java.util.Base64.getEncoder().encodeToString(fileBytes);
    send(RequestType.UPLOAD_IMAGE, new UploadImageRequest(itemId, base64Data, imageFile.getName()));
  }

  public void fetchUserList() throws IOException {
    send(RequestType.FETCH_USER_LIST, null);
  }

  public void uploadAvatar(java.io.File imageFile) throws IOException {
    byte[] fileBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());
    String base64Data = java.util.Base64.getEncoder().encodeToString(fileBytes);
    send(RequestType.UPLOAD_AVATAR, new UploadAvatarRequest(base64Data, imageFile.getName()));
  }

  public void fetchAvatar(int userId, String avatarUrl) throws IOException {
    send(RequestType.FETCH_AVATAR, new FetchAvatarRequest(userId, avatarUrl));
  }

  /** setAutoBid. */
  public void setAutoBid(int auctionId, long maxAmount, long incrementAmount) throws IOException {
    send(RequestType.SET_AUTO_BID, new SetAutoBidRequest(auctionId, maxAmount, incrementAmount));
  }

  /** disableAutoBid. */
  public void disableAutoBid(int auctionId) throws IOException {
    send(RequestType.DISABLE_AUTO_BID, new DisableAutoBidRequest(auctionId));
  }

  private void send(RequestType type, Request payload) throws IOException {
    client.sendRequest(PacketReq.of(type, payload));
  }
}
