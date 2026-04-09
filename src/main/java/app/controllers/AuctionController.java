package app.controllers;

import Common.Auction;
import Common.Item;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AuctionController {

    @FXML private TextField nameField;
    @FXML private TextField categoryField;
    @FXML private TextField priceField;
    @FXML private TextField minutesField;

    @FXML private TextField idField;
    @FXML private TextField bidderField;
    @FXML private TextField amountField;

    @FXML private TextArea outputArea;

    // ===== NÚT ADD =====
    @FXML
    public void handleAdd() {
        try {
            String name = nameField.getText();
            String cat = categoryField.getText();
            double price = Double.parseDouble(priceField.getText());
            int minutes = Integer.parseInt(minutesField.getText());

            Auction.getInstance().addItem(name, cat, price, minutes);

            outputArea.setText("Đã thêm item!");
        } catch (Exception e) {
            outputArea.setText("Lỗi nhập!");
        }
    }

    // ===== NÚT BID =====
    @FXML
    public void handleBid() {
        try {
            int id = Integer.parseInt(idField.getText());
            String user = bidderField.getText();
            double amount = Double.parseDouble(amountField.getText());

            Bid.BidResult result = Auction.getInstance().bidItem(id, user, amount);

            outputArea.setText(result.message);
        } catch (Exception e) {
            outputArea.setText("Lỗi dữ liệu!");
        }
    }

    // ===== NÚT SHOW =====
    @FXML
    public void handleShow() {
        outputArea.setText(Auction.getInstance().getItemsAsString());
    }
}