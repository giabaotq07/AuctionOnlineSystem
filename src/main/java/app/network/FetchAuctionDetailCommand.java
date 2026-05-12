package app.network;

import app.data.AuctionDetail;
import app.data.AuctionDetailRequest;
import app.data.AuctionDetailResponse;
import app.enums.PacketType;
import app.models.PacketReq;
import app.models.PacketRes;
import app.service.AuctionService;

public class FetchAuctionDetailCommand implements Command {

    private final AuctionService auctionService;

    public FetchAuctionDetailCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public void execute(ClientHandler clientHandler, PacketReq packet) {
        AuctionDetailRequest request = packet.getData(AuctionDetailRequest.class);

        try {
            AuctionDetail detail = auctionService.getAuctionDetail(request.auctionId());
            AuctionDetailResponse response = new AuctionDetailResponse(true, "OK", detail);
            clientHandler.sendMessage(PacketRes.of(PacketType.FETCH_AUCTION_DETAIL, response));
        } catch (app.exception.ServiceException e) {
            AuctionDetailResponse response =
                new AuctionDetailResponse(false, e.getMessage(), null);
            clientHandler.sendMessage(PacketRes.of(PacketType.FETCH_AUCTION_DETAIL, response));
        }
    }
}
