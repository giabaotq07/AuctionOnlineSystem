package app.network;

import app.data.AuctionResultRequest;
import app.data.AuctionResultResponse;
import app.enums.PacketType;
import app.models.PacketReq;
import app.models.PacketRes;
import app.service.AuctionService;

public class FetchAuctionResultCommand implements Command {

    private final AuctionService auctionService;

    public FetchAuctionResultCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public void execute(ClientHandler clientHandler, PacketReq packet) {
        AuctionResultRequest request = packet.getData(AuctionResultRequest.class);
        AuctionResultResponse response = auctionService.getAuctionResult(request.auctionId());
        clientHandler.sendMessage(PacketRes.of(PacketType.FETCH_AUCTION_RESULT, response));
    }
}
