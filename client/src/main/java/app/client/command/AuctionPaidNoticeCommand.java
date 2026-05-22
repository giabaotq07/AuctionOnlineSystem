package app.client.command;

import app.client.utils.AlertUtils;
import app.common.dto.AuctionPaidNoticeResponse;
import app.common.protocol.PacketRes;
import java.text.DecimalFormat;
import javafx.application.Platform;

/** AuctionPaidNoticeCommand. */
public class AuctionPaidNoticeCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet == null || !packet.isSuccess()) {
      return;
    }
    AuctionPaidNoticeResponse response = packet.getData(AuctionPaidNoticeResponse.class);
    if (response == null) {
      return;
    }
    String roleLabel =
        "WINNER".equalsIgnoreCase(response.role()) ? "Bạn đã thanh toán" : "Bạn đã nhận thanh toán";
    DecimalFormat formatter = new DecimalFormat("#,###");
    String amountText = response.amount() == null ? "0" : formatter.format(response.amount());
    String content =
        roleLabel + "\nPhiên: " + response.auctionName() + "\nSố tiền: " + amountText + " đ";
    Platform.runLater(() -> AlertUtils.showInfo("Thanh toán thành công", content));
  }
}
