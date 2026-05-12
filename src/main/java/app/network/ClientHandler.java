package app.network;

import app.enums.PacketType;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.User;
import app.service.*;
import app.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;
    private User user;
    private String username;

    private final AuctionService auctionService;
    private final BidService bidService;
    private final UserService userService;
    private final ItemService itemService;

    public ClientHandler(Socket socket, AuctionService auctionService, BidService bidService, UserService userService, ItemService itemService) {
        this.socket = socket;
        this.auctionService = auctionService;
        this.bidService = bidService;
        this.userService = userService;
        this.itemService = itemService;
    }

    @Override
    public void run() {
        try {
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            writer.flush();
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            listen();
        } catch (IOException e) {
            logger.error("Error initializing client handler", e);
            close();
        }
    }

    private void listen() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    PacketReq packet = JsonUtil.fromJson(line, PacketReq.class);
                    handlePacket(packet);
                } catch (Exception e) {
                    logger.error("Invalid packet received: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error("Error while listening to client: {}", e.getMessage());
        } finally {
            close();
        }
    }

    private void handlePacket(PacketReq packet) {
        PacketType type = packet.getType();
        if (type == null) {
            logger.warn("Unrecognized command type: null");
            return;
        }
        logger.info("Processing command: {}", type);
        Command command;
        switch (type) {
            case LOGIN:
                command = new LoginCommand(userService);
                break;
            case REGISTER:
                command = new RegisterCommand(userService);
                break;
            case CREATE_AUCTION:
                command = new CreateAuctionCommand(auctionService, itemService);
                break;
            case FETCH_AUCTIONS:
                command = new FetchAuctionsCommand(auctionService);
                break;
            case FETCH_HISTORY:
                command = new FetchHistoryCommand(auctionService);
                break;
            case FETCH_AUCTION_DETAIL:
                command = new FetchAuctionDetailCommand(auctionService);
                break;
            case FETCH_AUCTION_RESULT:
                command = new FetchAuctionResultCommand(auctionService);
                break;
            case PLACE_BID:
                command = new PlaceBidCommand(bidService);
                break;
            default:
                logger.warn("Unrecognized command type: {}", type);
                return;
        }
        command.execute(this, packet);
    }

    public void sendMessage(PacketRes packet) {
        if (writer != null) {
            try {
                writer.write(JsonUtil.toJson(packet));
                writer.newLine();
                writer.flush();
                logger.debug("Sent message to {}", username);
            } catch (IOException e) {
                logger.error("Failed to send message to {}: {}", username, e.getMessage());
            }
        }
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        this.username = user.getName();
    }

    public Socket getSocket() {
        return socket;
    }

    public BufferedWriter getWriter() {
        return writer;
    }

    public BufferedReader getReader() {
        return reader;
    }

    public String getUsername() {
        return username;
    }

    public AuctionService getAuctionService() {
        return auctionService;
    }

    public BidService getBidService() {
        return bidService;
    }

    public UserService getUserService() {
        return userService;
    }

    public ItemService getItemService() {
        return itemService;
    }

    private void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
