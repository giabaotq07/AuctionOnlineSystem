package app.controllers;

import app.config.AlertUtils;
import app.config.NavigationManager;
import app.config.View;
import app.dao.HistoryDAO;
import app.models.HistoryRecord;
import app.network.Client;

import java.io.IOException;
import java.util.List;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MyHistoryController {

    @FXML private HBox historyContainerPane;

    private static final double CARD_WIDTH = 280;
    private static final double SPACING = 20;

    private final HistoryDAO histories = HistoryDAO.getInstance();

    @FXML
    public void initialize() {
        System.out.println("INIT " + this);


        List<HistoryRecord> list = histories.getAllHistory();

        historyContainerPane.getChildren().clear();

        historyContainerPane.getChildren().setAll(createContainer(list));
    }

    // ================= CONTAINER (HBOX NGANG) =================

    private HBox createContainer(List<HistoryRecord> records) {

        HBox container = new HBox();

        container.setAlignment(Pos.CENTER_LEFT);
        container.setSpacing(SPACING);

        for (HistoryRecord r : records) {
            container.getChildren().add(createHistoryCard(r));
        }

        return container;
    }

    // ================= CARD =================

    private VBox createHistoryCard(HistoryRecord record) {

        VBox box = new VBox();

        box.setPrefWidth(CARD_WIDTH);
        box.setMinWidth(CARD_WIDTH);
        box.setMaxWidth(CARD_WIDTH);

        box.setStyle(
                "-fx-background-color: #1a1f35;"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: 15;"
                        + "-fx-spacing: 8;"
                        + "-fx-cursor: hand;"
        );

        Label type = new Label(record.getType().name());
        type.setStyle("-fx-text-fill: #e91e63; -fx-font-weight: bold;");

        Label message = new Label(record.getMessage());
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: white;");

        Label time = new Label(String.valueOf(record.getTime()));
        time.setStyle("-fx-text-fill: #9aa0b4; -fx-font-size: 11px;");

        box.getChildren().addAll(type, message, time);

        // ================= CLICK EVENT =================
        box.setOnMouseClicked(e -> openHistoryDetail(record));

        return box;
    }

    // ================= DETAIL CLICK =================

    private void openHistoryDetail(HistoryRecord record) {

        System.out.println("Clicked history: " + record.getMessage());

        // nếu sau này muốn mở scene detail:
        // NavigationManager.getInstance().navigateTo(View.HISTORY_DETAIL, c -> ...);

        AlertUtils.showInfo(
                record.getType().name(),
                record.getMessage()
        );
    }

    // ================= NAV =================

    @FXML
    public void SwitchToUI(javafx.event.ActionEvent event) throws IOException {

        if (!Client.getInstance().isConnected()) {
            AlertUtils.showError(
                    "Mất kết nối",
                    "Bạn đã mất kết nối tới server. Vui lòng kết nối lại!"
            );
            return;
        }

        NavigationManager.getInstance().navigateTo(View.UI);
    }
}