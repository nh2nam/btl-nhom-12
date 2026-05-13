package com.auction.client.controller;

import com.auction.client.Main;
import com.auction.client.model.AppData;
import com.auction.client.model.Item;
import com.auction.client.network.Response;
import com.auction.client.network.ServerConnection;
import com.auction.client.session.UserSession;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SellingProductController {

    @FXML private Label lblUsername;
    @FXML private ImageView imgProduct;
    @FXML private Label lblProductInfo;
    @FXML private Label lblProductName;
    @FXML private Label lblCurrentPrice;
    @FXML private TextField txtBidAmount;
    @FXML private Label description;
    @FXML private Button btnConfirm;
    @FXML private Text priceCheck;
    @FXML private VBox vboxBidHistory;

    private final String GRAY_STYLE  = "-fx-background-color: #888888; -fx-text-fill: black; -fx-border-color: black; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 10; -fx-border-radius: 10;";
    private final String GREEN_STYLE = "-fx-background-color: #28a745; -fx-text-fill: white; -fx-border-color: black; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 10; -fx-border-radius: 10;";

    private final Gson gson = new Gson();
    private Item currentItem;
    private int  currentAuctionId = -1;

    // Scheduler polling lịch sử mỗi 10 giây
    private ScheduledExecutorService pollingScheduler;

    @FXML
    public void initialize() {
        lblUsername.setText(AppData.username);
        setProductData(AppData.selectedAuction);

        btnConfirm.setStyle(GRAY_STYLE);
        btnConfirm.setDisable(true);

        txtBidAmount.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean empty = newVal.trim().isEmpty();
            btnConfirm.setStyle(empty ? GRAY_STYLE : GREEN_STYLE);
            btnConfirm.setDisable(empty);
        });

        // Bắt đầu polling lịch sử đặt giá mỗi 10 giây
        startPolling();
    }

    public void setProductData(Map<String, Object> selectedAuction) {
        if (selectedAuction == null) return;
        try {
            String name = String.valueOf(selectedAuction.getOrDefault("name", "Sản phẩm không tên"));
            lblProductName.setText(name);

            Object bidObj = selectedAuction.get("startingPrice");
            double price = (bidObj instanceof Number) ? ((Number) bidObj).doubleValue() : 0.0;
            lblCurrentPrice.setText(String.format("Giá hiện tại: %,.0f VNĐ", price));

            String desc = (String) selectedAuction.getOrDefault("description", "Không có mô tả.");
            String category = (String) selectedAuction.getOrDefault("category", "Other"); // Lấy category ra

            // Sửa lại dòng setText của lblProductInfo:
            description.setText(desc);
            lblProductInfo.setText("📌 Phân loại: " + category + "\n\n📝 Mô tả: " + desc);

            String path = (String) selectedAuction.get("itemImagePath");
            if (path != null && !path.trim().isEmpty()) {
                try {
                    if (path.length() > 500) {
                        byte[] imageBytes = Base64.getDecoder().decode(path);
                        imgProduct.setImage(new Image(new ByteArrayInputStream(imageBytes)));
                    } else if (path.startsWith("http") || path.startsWith("file:")) {
                        imgProduct.setImage(new Image(path));
                    } else {
                        java.io.InputStream is = getClass().getResourceAsStream(path);
                        if (is != null) imgProduct.setImage(new Image(is));
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi giải mã ảnh: " + e.getMessage());
                }
            }

            this.currentItem = new Item(name, price, path, desc);

            Object idObj = selectedAuction.get("id");
            this.currentAuctionId = (idObj instanceof Number) ? ((Number) idObj).intValue() : -1;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Đặt giá thủ công
    // -------------------------------------------------------------------------

    @FXML
    private void handleConfirmBid() {
        try {
            double newPrice = Double.parseDouble(txtBidAmount.getText().trim());

            if (newPrice <= currentItem.getPrice()) {
                priceCheck.setText("Giá phải lớn hơn giá hiện tại!");
                priceCheck.setStyle("-fx-fill: red;");
                return;
            }
            if (currentAuctionId == -1) {
                priceCheck.setText("Lỗi: Không xác định được phiên đấu giá!");
                priceCheck.setStyle("-fx-fill: red;");
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("auctionId", currentAuctionId);
            data.put("bidderId",  UserSession.getInstance().getUserId());
            data.put("amount",    newPrice);

            Response response = ServerConnection.getInstance().send("PLACE_BID", data);

            if (response.isSuccess()) {
                currentItem.setPrice(newPrice);
                lblCurrentPrice.setText(String.format("Giá hiện tại: %,.0f VNĐ", newPrice));
                priceCheck.setText("Đặt giá thành công!");
                priceCheck.setStyle("-fx-fill: green;");
                loadBidHistory(); // Refresh lịch sử ngay sau khi đặt
            } else {
                priceCheck.setText(response.getMessage());
                priceCheck.setStyle("-fx-fill: red;");
            }

        } catch (NumberFormatException e) {
            priceCheck.setText("Nhập sai! Hãy nhập vào 1 con số.");
            priceCheck.setStyle("-fx-fill: red;");
        }
        txtBidAmount.clear();
    }

    // -------------------------------------------------------------------------
    // Auto-Bid
    // -------------------------------------------------------------------------

    @FXML
    private void handleAutoBid() {
        if (currentAuctionId == -1) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không xác định được phiên đấu giá!");
            return;
        }

        // Dialog nhập maxAmount và increment
        Dialog<Map<String, Double>> dialog = new Dialog<>();
        dialog.setTitle("Bật Auto-Bid");
        dialog.setHeaderText("Thiết lập Auto-Bid cho phiên #" + currentAuctionId);

        ButtonType btnOk = new ButtonType("Bật Auto-Bid", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOk, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 10;");
        TextField txtMax = new TextField();
        txtMax.setPromptText("Giá tối đa ($)");
        TextField txtIncrement = new TextField();
        txtIncrement.setPromptText("Bước giá mỗi lần ($)");
        content.getChildren().addAll(
                new Label("Giá tối đa bạn chấp nhận:"), txtMax,
                new Label("Bước tăng mỗi lần đặt:"), txtIncrement
        );
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == btnOk) {
                try {
                    Map<String, Double> result = new HashMap<>();
                    result.put("maxAmount",  Double.parseDouble(txtMax.getText().trim()));
                    result.put("increment",  Double.parseDouble(txtIncrement.getText().trim()));
                    return result;
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(values -> {
            Map<String, Object> data = new HashMap<>();
            data.put("auctionId", currentAuctionId);
            data.put("bidderId",  UserSession.getInstance().getUserId());
            data.put("maxAmount", values.get("maxAmount"));
            data.put("increment", values.get("increment"));

            Response response = ServerConnection.getInstance().send("REGISTER_AUTO_BID", data);
            showAlert(
                    response.isSuccess() ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                    response.isSuccess() ? "Thành công" : "Thất bại",
                    response.getMessage()
            );
            if (response.isSuccess()) loadBidHistory();
        });
    }

    // -------------------------------------------------------------------------
    // Lịch sử đặt giá
    // -------------------------------------------------------------------------

    private void loadBidHistory() {
        if (currentAuctionId == -1) return;

        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", currentAuctionId);

        Response response = ServerConnection.getInstance().send("GET_BID_HISTORY", data);
        if (!response.isSuccess()) return;

        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> history;
        try {
            history = gson.fromJson(response.getData(), listType);
        } catch (Exception e) {
            return;
        }

        // Cập nhật UI trên JavaFX thread
        Platform.runLater(() -> {
            vboxBidHistory.getChildren().clear();
            if (history == null || history.isEmpty()) {
                vboxBidHistory.getChildren().add(new Label("Chưa có lịch sử đặt giá."));
                return;
            }

            // Lấy giá cao nhất từ lịch sử để cập nhật label giá hiện tại
            double highestBid = history.stream()
                    .mapToDouble(bid -> ((Number) bid.get("amount")).doubleValue())
                    .max()
                    .orElse(currentItem != null ? currentItem.getPrice() : 0.0);

            if (currentItem != null && highestBid > currentItem.getPrice()) {
                currentItem.setPrice(highestBid);
                lblCurrentPrice.setText(String.format("Giá hiện tại: %,.0f VNĐ", highestBid));
            }

            // Hiển thị mới nhất lên đầu
            for (int i = history.size() - 1; i >= 0; i--) {
                Map<String, Object> bid = history.get(i);
                double amount      = ((Number) bid.get("amount")).doubleValue();
                String bidTime     = String.valueOf(bid.get("bidTime"));
                String bidderName  = bid.get("bidderName") != null
                        ? String.valueOf(bid.get("bidderName"))
                        : "Bidder #" + ((Number) bid.get("bidderId")).intValue();

                Label lbl = new Label(String.format("%s  —  %,.0f$  —  %s", bidderName, amount, bidTime));
                lbl.setStyle("-fx-font-size: 13px; -fx-padding: 4 8; -fx-background-color: #f0f0f0; -fx-background-radius: 5;");
                vboxBidHistory.getChildren().add(lbl);
            }
        });
    }

    /** Polling tự động mỗi 10 giây để cập nhật lịch sử */
    private void startPolling() {
        pollingScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "bid-history-poller");
            t.setDaemon(true); // Tự tắt khi app đóng
            return t;
        });
        pollingScheduler.scheduleAtFixedRate(this::loadBidHistory, 1, 5, TimeUnit.SECONDS);
    }

    /** Dừng polling khi rời màn hình */
    private void stopPolling() {
        if (pollingScheduler != null && !pollingScheduler.isShutdown()) {
            pollingScheduler.shutdown();
        }
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------
    @FXML private ContextMenu userMenu, auctionMenu, sessionMenu;

    // Hàm chung để hiện menu khi di chuột vào Label
    @FXML
    private void handleShowUserMenu(MouseEvent event) {
        Label src = (Label) event.getSource();
        userMenu.show(src, Side.BOTTOM, 0, 0);
    }

    @FXML
    private void handleShowAuctionMenu(MouseEvent event) {
        Label src = (Label) event.getSource();
        auctionMenu.show(src, Side.BOTTOM, 0, 0);
    }

    @FXML
    private void handleShowSessionMenu(MouseEvent event) {
        Label src = (Label) event.getSource();
        sessionMenu.show(src, Side.BOTTOM, 0, 0);
    }

    @FXML
    private void addNewProduct() {
        com.auction.client.Main.changeScene("/view/add_product.fxml");
    }

    @FXML
    private void logout() {
        stopPolling();
        Main.changeScene("/view/login.fxml");
    }

    @FXML
    private void goHome() {
        stopPolling();
        Main.changeScene("/view/seller.fxml");
    }

    // -------------------------------------------------------------------------
    // Tiện ích
    // -------------------------------------------------------------------------

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
