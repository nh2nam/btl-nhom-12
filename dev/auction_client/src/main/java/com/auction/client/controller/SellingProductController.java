package com.auction.client.controller;

import com.auction.client.Main;
import com.auction.client.model.AppData;
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
    @FXML private Label lblStartingPrice;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblEndTime;
    @FXML private TextField txtBidAmount;
    @FXML private Label description;
    @FXML private Button btnConfirm;
    @FXML private Text priceCheck;
    @FXML private VBox vboxBidHistory;

    private final String GRAY_STYLE  = "-fx-background-color: #888888; -fx-text-fill: black; -fx-border-color: black; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 10; -fx-border-radius: 10;";
    private final String GREEN_STYLE = "-fx-background-color: #28a745; -fx-text-fill: white; -fx-border-color: black; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 10; -fx-border-radius: 10;";

    private final Gson gson = new Gson();
    private double currentHighestBid;
    private int  currentAuctionId = -1;

    // Scheduler polling lịch sử mỗi 10 giây
    private ScheduledExecutorService pollingScheduler;

    @FXML
    public void initialize() {
        lblUsername.setText(UserSession.getInstance().getDisplayName());
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

            Object startingPriceObj = selectedAuction.get("startingPrice");
            double startingPrice = (startingPriceObj instanceof Number) ? ((Number) startingPriceObj).doubleValue() : 0.0;
            lblStartingPrice.setText(String.format("%,.0f VNĐ", startingPrice));

            Object currentBidObj = selectedAuction.get("currentHighestBid");
            currentHighestBid = (currentBidObj instanceof Number) ? ((Number) currentBidObj).doubleValue() : startingPrice;
            lblCurrentPrice.setText(String.format("%,.0f VNĐ", currentHighestBid));

            String endTime = (String) selectedAuction.getOrDefault("endTime", "");
            lblEndTime.setText(endTime.isEmpty() ? "Không xác định" : endTime);

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

            if (newPrice <= currentHighestBid) {
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
                currentHighestBid = newPrice;
                lblCurrentPrice.setText(String.format("%,.0f VNĐ", newPrice));
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
        txtMax.setPromptText("Giá tối đa (VND)");
        TextField txtIncrement = new TextField();
        txtIncrement.setPromptText("Bước giá mỗi lần (VND)");
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

    @FXML
    private void handleShowBidHistory() {
        if (currentAuctionId == -1) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không xác định được phiên đấu giá!");
            return;
        }

        // Tải lịch sử mới nhất trước khi hiện dialog
        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", currentAuctionId);
        Response response = ServerConnection.getInstance().send("GET_BID_HISTORY", data);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Lịch sử đấu giá");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Style dialog pane tối
        dialog.getDialogPane().setStyle(
                "-fx-background-color: #2a2a2a;"
        );

        VBox container = new VBox(12);
        container.setStyle("-fx-padding: 20; -fx-background-color: #2a2a2a;");
        container.setPrefWidth(480);

        Label title = new Label("LỊCH SỬ ĐẤU GIÁ");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #555;");

        VBox listBox = new VBox(8);
        listBox.setStyle("-fx-padding: 4 0 0 0;");

        if (!response.isSuccess()) {
            Label err = new Label("Không thể tải lịch sử đấu giá.");
            err.setStyle("-fx-text-fill: #ff5252; -fx-font-size: 14px;");
            listBox.getChildren().add(err);
        } else {
            Type listType = new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> history;
            try {
                history = gson.fromJson(response.getData(), listType);
            } catch (Exception e) {
                history = null;
            }

            if (history == null || history.isEmpty()) {
                Label empty = new Label("Chưa có lịch sử đặt giá.");
                empty.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 14px;");
                listBox.getChildren().add(empty);
            } else {
                for (int i = history.size() - 1; i >= 0; i--) {
                    Map<String, Object> bid = history.get(i);
                    double amount     = ((Number) bid.get("amount")).doubleValue();
                    String bidTime    = String.valueOf(bid.get("bidTime"));
                    String bidderName = bid.get("bidderName") != null
                            ? String.valueOf(bid.get("bidderName"))
                            : "Bidder #" + ((Number) bid.get("bidderId")).intValue();

                    Label row = new Label(String.format("👤 %s   —   %,.0f VNĐ   —   %s", bidderName, amount, bidTime));
                    row.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 14px; -fx-padding: 6 10; " +
                            "-fx-background-color: #1e1e1e; -fx-background-radius: 6;");
                    row.setMaxWidth(Double.MAX_VALUE);
                    listBox.getChildren().add(row);
                }
            }
        }

        ScrollPane scrollPane = new ScrollPane(listBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(350);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #2a2a2a; -fx-border-color: transparent;");

        container.getChildren().addAll(title, sep, scrollPane);
        dialog.getDialogPane().setContent(container);

        // Style nút Close
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE)
                .setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        dialog.showAndWait();
    }

    // -------------------------------------------------------------------------
    // Biểu đồ giá
    // -------------------------------------------------------------------------

    @FXML
    private void handleShowPriceChart() {
        if (currentAuctionId == -1) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không xác định được phiên đấu giá!");
            return;
        }

        // Lấy lịch sử từ server
        Map<String, Object> req = new HashMap<>();
        req.put("auctionId", currentAuctionId);
        Response response = ServerConnection.getInstance().send("GET_BID_HISTORY", req);

        // Parse dữ liệu
        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> history = null;
        if (response.isSuccess()) {
            try {
                history = gson.fromJson(response.getData(), listType);
            } catch (Exception ignored) {}
        }

        // Trục X: thứ tự lượt đặt, Trục Y: giá
        javafx.scene.chart.NumberAxis xAxis = new javafx.scene.chart.NumberAxis();
        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        xAxis.setLabel("Lượt đặt giá");
        yAxis.setLabel("Giá (VNĐ)");
        xAxis.setTickUnit(1);
        xAxis.setMinorTickVisible(false);

        // Tính giá max để set upper bound có khoảng trống cho label
        final List<Map<String, Object>> finalHistory = history;
        double maxPrice = currentHighestBid;
        if (finalHistory != null && !finalHistory.isEmpty()) {
            for (Map<String, Object> bid : finalHistory) {
                double a = ((Number) bid.get("amount")).doubleValue();
                if (a > maxPrice) maxPrice = a;
            }
        }

        // Số điểm dữ liệu
        int dataCount = (finalHistory != null && !finalHistory.isEmpty()) ? finalHistory.size() : 1;

        // Trục X: thêm 1.5 đơn vị padding bên phải để label điểm cuối không bị khuất
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(dataCount + 1.5);
        xAxis.setTickUnit(Math.max(1, dataCount / 10.0));

        // Trục Y: upper bound = max + 15% để label trên đỉnh không bị cắt
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(maxPrice * 1.15);
        yAxis.setTickUnit(maxPrice * 1.15 / 8);

        javafx.scene.chart.LineChart<Number, Number> lineChart =
                new javafx.scene.chart.LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Biến động giá đấu giá");
        lineChart.setAnimated(false);
        lineChart.setLegendVisible(false);
        lineChart.setPrefSize(720, 430);
        lineChart.setStyle("-fx-background-color: #1e1e1e;");

        javafx.scene.chart.XYChart.Series<Number, Number> series =
                new javafx.scene.chart.XYChart.Series<>();
        series.setName("Giá đặt");

        if (finalHistory != null && !finalHistory.isEmpty()) {
            for (int i = 0; i < finalHistory.size(); i++) {
                Map<String, Object> bid = finalHistory.get(i);
                double amount = ((Number) bid.get("amount")).doubleValue();

                // Tạo node tùy chỉnh: chấm xanh + label giá phía trên
                javafx.scene.layout.StackPane dotWithLabel = makeDotNode(amount);

                javafx.scene.chart.XYChart.Data<Number, Number> dp =
                        new javafx.scene.chart.XYChart.Data<>(i + 1, amount);
                dp.setNode(dotWithLabel);
                series.getData().add(dp);
            }
        } else {
            javafx.scene.layout.StackPane dotWithLabel = makeDotNode(currentHighestBid);
            javafx.scene.chart.XYChart.Data<Number, Number> dp =
                    new javafx.scene.chart.XYChart.Data<>(1, currentHighestBid);
            dp.setNode(dotWithLabel);
            series.getData().add(dp);
        }

        lineChart.getData().add(series);

        // CSS cho chart tối
        lineChart.getStylesheets().add(
                "data:text/css," +
                ".chart-plot-background{-fx-background-color:#2a2a2a;}" +
                ".chart-title{-fx-text-fill:#ffffff;-fx-font-size:15px;-fx-font-weight:bold;}" +
                ".axis-label{-fx-text-fill:#dddddd;-fx-font-size:12px;}" +
                ".axis{-fx-tick-label-fill:#cccccc;}" +
                ".chart-legend{-fx-background-color:#2a2a2a;-fx-alignment:center;}" +
                ".chart-legend-item{-fx-text-fill:#FFD54F;-fx-font-size:13px;-fx-font-weight:bold;}" +
                ".default-color0.chart-series-line{-fx-stroke:#4FC3F7;-fx-stroke-width:2.5px;}" +
                ".default-color0.chart-line-symbol{-fx-background-color:transparent;}" +
                ".chart-vertical-grid-lines{-fx-stroke:#3a3a3a;}" +
                ".chart-horizontal-grid-lines{-fx-stroke:#3a3a3a;}"
        );

        // Bọc chart trong HBox để legend căn giữa toàn bộ chiều ngang
        javafx.scene.layout.HBox chartWrapper = new javafx.scene.layout.HBox(lineChart);
        chartWrapper.setAlignment(javafx.geometry.Pos.CENTER);
        javafx.scene.layout.HBox.setHgrow(lineChart, javafx.scene.layout.Priority.ALWAYS);

        // Dialog
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Biểu đồ giá — " + lblProductName.getText());
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color: #1e1e1e;");

        VBox wrapper = new VBox(10);
        wrapper.setStyle("-fx-padding: 15; -fx-background-color: #1e1e1e;");

        Label titleLbl = new Label("BIỂU ĐỒ GIÁ ĐẤU GIÁ");
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #555;");

        wrapper.getChildren().addAll(titleLbl, sep, chartWrapper);
        dialog.getDialogPane().setContent(wrapper);

        dialog.getDialogPane().lookupButton(ButtonType.CLOSE)
                .setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        dialog.showAndWait();
    }

    /**
     * Tạo node cho một điểm dữ liệu: chấm xanh tròn + label giá vàng phía trên.
     * Label được đặt trong cùng StackPane với chấm, dịch lên trên bằng translateY.
     */
    private javafx.scene.layout.StackPane makeDotNode(double amount) {
        // Chấm xanh
        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(5);
        dot.setFill(javafx.scene.paint.Color.web("#4FC3F7"));
        dot.setStroke(javafx.scene.paint.Color.WHITE);
        dot.setStrokeWidth(1);

        // Label giá
        String priceText = formatPrice(amount);
        Label lbl = new Label(priceText);
        lbl.setStyle(
                "-fx-text-fill: #FFD54F;" +
                "-fx-font-size: 9px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-color: rgba(0,0,0,0.0);" +
                "-fx-padding: 0;"
        );
        lbl.setMouseTransparent(true);
        // Dịch label lên trên tâm chấm: bán kính chấm (5) + khoảng cách (3) + nửa chiều cao label (~7) ≈ 20px
        lbl.setTranslateY(-20);

        javafx.scene.layout.StackPane sp = new javafx.scene.layout.StackPane(dot, lbl);
        sp.setPickOnBounds(false);
        return sp;
    }

    /** Format số tiền gọn */
    private String formatPrice(double amount) {
        if (amount >= 1_000_000_000) {
            return String.format("%.1fB", amount / 1_000_000_000);
        } else if (amount >= 1_000_000) {
            return String.format("%.1fM", amount / 1_000_000);
        } else if (amount >= 1_000) {
            return String.format("%.1fK", amount / 1_000);
        } else {
            return String.format("%,.0f", amount);
        }
    }

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
                    .orElse(currentHighestBid);

            if (highestBid > currentHighestBid) {
                currentHighestBid = highestBid;
                lblCurrentPrice.setText(String.format("%,.0f VNĐ", highestBid));
            }

            // Hiển thị mới nhất lên đầu
            for (int i = history.size() - 1; i >= 0; i--) {
                Map<String, Object> bid = history.get(i);
                double amount      = ((Number) bid.get("amount")).doubleValue();
                String bidTime     = String.valueOf(bid.get("bidTime"));
                String bidderName  = bid.get("bidderName") != null
                        ? String.valueOf(bid.get("bidderName"))
                        : "Bidder #" + ((Number) bid.get("bidderId")).intValue();

                Label lbl = new Label(String.format("%s  —  %,.0fVND  —  %s", bidderName, amount, bidTime));
                lbl.setStyle("-fx-font-size: 13px; -fx-padding: 4 0; -fx-text-fill: #cccccc;");
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
        Main.changeScene("/view/home.fxml");
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
