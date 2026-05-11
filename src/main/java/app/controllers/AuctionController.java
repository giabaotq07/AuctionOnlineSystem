package app.controllers;

import app.config.NavigationManager;
import app.data.CreateAuctionRequest;
import app.data.CreateAuctionResponse;
import app.data.Response;
import app.enums.ItemType;
import app.enums.PacketType;
import app.enums.View;
import app.models.DataStore;
import app.models.PacketReq;
import app.network.Client;
import app.utils.AlertUtils;
import java.io.IOException;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AuctionController {

  @FXML private TextField nameField;
  @FXML private TextArea descriptionField;
  @FXML private TextField startingPriceField;
  @FXML private TextField stepPriceField;
  @FXML private ComboBox<ItemType> typeComboBox;
  @FXML private TextField durationField;
  private Consumer<Response> createAuctionHandler;

  @FXML
  public void initialize() {

    if (Client.getInstance().getCurrentUser() == null) {
      AlertUtils.showError("Chưa đăng nhập", "Bạn phải đăng nhập để tổ chức phiên đấu giá!");
      Platform.runLater(() -> NavigationManager.getInstance().navigateTo(View.LOGIN));
      return;
    }

    if (typeComboBox != null) {
      typeComboBox.getItems().setAll(ItemType.values());
      typeComboBox.getSelectionModel().selectFirst();
    }

    createAuctionHandler =
        response ->
            Platform.runLater(
                () -> {
                  if (!(response instanceof CreateAuctionResponse)) {
                    return;
                  }
                  CreateAuctionResponse createAuctionResponse = (CreateAuctionResponse) response;
                  if (createAuctionResponse.success()) {
                    if (createAuctionResponse.auction() != null) {
                      try {
                        DataStore.getInstance().sessions.add(createAuctionResponse.auction());
                      } catch (IOException e) {
                        AlertUtils.showError("Lỗi", createAuctionResponse.message());
                      }
                    }
                    AlertUtils.showInfo("OK", createAuctionResponse.message());
                    if (createAuctionHandler != null) {
                      Client.getInstance()
                          .unsubscribe(PacketType.CREATE_AUCTION, createAuctionHandler);
                    }
                    NavigationManager.getInstance().navigateTo(View.UI);
                  } else {
                    AlertUtils.showError("Lỗi", createAuctionResponse.message());
                  }
                });
    Client.getInstance().subscribe(PacketType.CREATE_AUCTION, createAuctionHandler);
  }

  @FXML
  public void handleAdd(ActionEvent event) {

    if (!Client.getInstance().connected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server.");
      return;
    }

    if (Client.getInstance().getCurrentUser() == null) {
      AlertUtils.showError("Chưa đăng nhập", "Bạn phải đăng nhập!");
      NavigationManager.getInstance().navigateTo(View.LOGIN);
      return;
    }

    try {

      String name = nameField.getText();
      String desc = descriptionField.getText();
      long startPrice = Long.parseLong(startingPriceField.getText());
      long stepPrice = Long.parseLong(stepPriceField.getText());
      int durationMins = Integer.parseInt(durationField.getText());
      ItemType type = typeComboBox.getValue();

      if (name.isEmpty() || desc.isEmpty()) {
        AlertUtils.showError("Lỗi", "Thiếu thông tin.");
        return;
      }

      CreateAuctionRequest request =
          new CreateAuctionRequest(
              name,
              desc,
              startPrice,
              stepPrice,
              type,
              durationMins,
              Client.getInstance().getCurrentUser().getId());
      Client.getInstance().sendRequest(PacketReq.of(PacketType.CREATE_AUCTION, request));

    } catch (NumberFormatException e) {
      AlertUtils.showError("Sai định dạng", "Giá / thời gian phải là số");
    } catch (IOException e) {
      AlertUtils.showError("Lỗi", "Server không phản hồi");
    } catch (Exception e) {
      AlertUtils.showError("Lỗi", e.getMessage());
      e.printStackTrace();
    }
  }

  @FXML
  public void handleBack(ActionEvent event) {
    if (createAuctionHandler != null) {
      Client.getInstance().unsubscribe(PacketType.CREATE_AUCTION, createAuctionHandler);
    }
    NavigationManager.getInstance().navigateTo(View.UI);
  }
}
