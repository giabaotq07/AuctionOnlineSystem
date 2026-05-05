package app.controllers;

import app.config.AlertUtils;
import app.config.NavigationManager;
import app.config.View;
import app.dao.AuctionDAO;
import app.dao.BidDAO;
import app.dao.HistoryDAO;
import app.enums.HistoryType;
import app.models.Auction;
import app.models.HistoryRecord;
import app.network.Client;
import app.service.BidService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyHistoryController {

    // Khớp fx:id từ file FXML của ông
    @FXML private ScrollPane historyScrollPane;
    @FXML private HBox historyContainerPane;

    private static final double CARD_WIDTH = 280;
    private static final double SPACING = 20; // Khớp với spacing="20" trong FXML

    // ✅ Thêm BidService để lấy highest bid từ database
    private final BidService bidService = new BidService(BidDAO.getInstance());

    @FXML
    public void initialize() {
        // Đợi UI render xong rồi mới tính toán scroll
        Platform.runLater(() -> {
            refreshHistoryContainer();
            startAutoScroll();
        });
    }

    private void refreshHistoryContainer() {
        if (historyContainerPane == null) return;
        historyContainerPane.getChildren().clear();

        List<HistoryRecord> allHistory = HistoryDAO.getInstance().getAllHistory();
        List<Auction> allAuctions = AuctionDAO.getInstance().getAllAuction();

        Set<Integer> processedAuctionIds = new HashSet<>();

        for (HistoryRecord record : allHistory) {
            // Logic lọc theo HistoryType của ông
            if (record.getType() == HistoryType.BID || record.getType() == HistoryType.ADD_ITEM) {
                int auctionId = record.getSessionId();

                if (!processedAuctionIds.contains(auctionId)) {
                    Auction session = allAuctions.stream()
                            .filter(a -> a.getId() == auctionId)
                            .findFirst()
                            .orElse(null);

                    if (session != null) {
                        historyContainerPane.getChildren().add(createAuctionCard(session, record.getType()));
                        processedAuctionIds.add(auctionId);
                    }
                }
            }
        }
    }

    private VBox createAuctionCard(Auction session, HistoryType type) {
        VBox vbox = new VBox();
        vbox.setPrefWidth(CARD_WIDTH);
        vbox.setMinWidth(CARD_WIDTH);
        vbox.setMaxWidth(CARD_WIDTH);
        vbox.setStyle("-fx-background-color: #1a1f35; -fx-background-radius: 8; -fx-padding: 15; -fx-spacing: 10;");

        Label badge = new Label(type == HistoryType.ADD_ITEM ? "✪ ĐỒ CỦA TÔI" : "✔ ĐÃ THAM GIA");
        badge.setStyle(type == HistoryType.ADD_ITEM ? "-fx-text-fill: #00ff88; -fx-font-weight: bold;" : "-fx-text-fill: #00ccff; -fx-font-weight: bold;");

        Label titleLabel = new Label(session.getItem().getName());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");
        titleLabel.setWrapText(true);

        // ✅ Lấy giá cao nhất từ database thay vì in-memory
        double highestBid = bidService.getHighestBidAmount(session.getId());
        double displayPrice = (highestBid > 0) ? highestBid : session.getItem().getStartingPrice();
        Label priceLabel = new Label(String.format("Giá: %,.0f đ", displayPrice));
        priceLabel.setStyle("-fx-text-fill: #e91e63; -fx-font-weight: bold;");

        Button btnDetail = new Button("Chi tiết");
        btnDetail.setMaxWidth(Double.MAX_VALUE);
        btnDetail.setStyle("-fx-background-color: #673ab7; -fx-text-fill: white; -fx-cursor: hand;");
        btnDetail.setOnAction(e -> handleGoToLive(session));

        vbox.getChildren().addAll(badge, titleLabel, priceLabel, btnDetail);
        return vbox;
    }

    private void startAutoScroll() {
        if (historyScrollPane == null || historyContainerPane == null) return;

        Timeline scrollTimeline = new Timeline(
                new KeyFrame(Duration.seconds(3), e -> {
                    double contentWidth = historyContainerPane.getWidth();
                    double viewWidth = historyScrollPane.getViewportBounds().getWidth();
                    double maxScroll = contentWidth - viewWidth;

                    if (maxScroll <= 0) return;

                    double step = CARD_WIDTH + SPACING;
                    double nextPixel = (historyScrollPane.getHvalue() * maxScroll) + step;

                    if (nextPixel >= maxScroll + 10) { // Thêm tí đệm để reset mượt
                        nextPixel = 0;
                    }
                    historyScrollPane.setHvalue(nextPixel / maxScroll);
                })
        );
        scrollTimeline.setCycleCount(Timeline.INDEFINITE);
        scrollTimeline.play();

        historyScrollPane.setOnMouseEntered(ev -> scrollTimeline.pause());
        historyScrollPane.setOnMouseExited(ev -> scrollTimeline.play());
    }

    private void handleGoToLive(Auction session) {
        try {
            NavigationManager.getInstance().navigateTo(View.LIVE, c -> {
                if (c instanceof LiveController) ((LiveController) c).setSession(session);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void SwitchToUI() {
        try {
            NavigationManager.getInstance().navigateTo(View.UI);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}