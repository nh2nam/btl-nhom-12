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
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SellingProductController {

    @FXML private Label lblUsername;
    @FXML private ImageView imgProduct;
    @FXML private HBox hboxThumbnails;
    @FXML private Label lblProductInfo;
    @FXML private Label lblProductName;
    @FXML private Label lblStartingPrice;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblEndTime;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnConfirm;
    @FXML private Text priceCheck;
    @FXML private VBox vboxBidHistory;

    // Danh sách URL ảnh của sản phẩm đang xem
    private final List<String> productImageUrls = new ArrayList<>();
    private int currentImageIndex = 0;

    private final String GRAY_STYLE  = "-fx-background-color: #888888; -fx-text-fill: black; -fx-border-color: black; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 10; -fx-border-radius: 10;";
    private final String GREEN_STYLE = "-fx-background-color: #28a745; -fx-text-fill: white; -fx-border-color: black; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 10; -fx-border-radius: 10;";

    private final Gson gson = new Gson();
    private double currentHighestBid;
    private int  currentAuctionId = -1;

    private java.net.Socket radioSocket; // Ống nghe sự kiện
    // OBSERVER: Biến lưu trữ giao diện Dialog để cập nhật Real-time
    private VBox dialogHistoryBox;
    private javafx.scene.chart.XYChart.Series<Number, Number> dialogChartSeries;
    private javafx.scene.chart.NumberAxis dialogXAxis;
    private javafx.scene.chart.NumberAxis dialogYAxis;

    // --- LIVE CHAT ---
    @FXML private VBox vboxChatMessages;
    @FXML private ScrollPane scrollPaneChat;
    @FXML private TextField txtChatInput;

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
        startListeningForPrices();
        // Tải lịch sử chat từ server khi vừa vào phòng
        new Thread(this::loadChatHistory).start();


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

            lblProductInfo.setText("📌 Phân loại: " + category + "\n\n📝 Mô tả: " + desc);

            // --- XỬ LÝ GALLERY ẢNH (hỗ trợ nhiều ảnh, phân cách bởi dấu phẩy) ---
            String rawPath = (String) selectedAuction.get("itemImagePath");
            productImageUrls.clear();
            currentImageIndex = 0;

            if (rawPath != null && !rawPath.trim().isEmpty()) {
                String[] parts = rawPath.split(",");
                for (String p : parts) {
                    String url = p.trim();
                    if (!url.isEmpty()) {
                        productImageUrls.add(url);
                    }
                }
            }

            // Hiển thị ảnh chính (ảnh đầu tiên)
            showImageAt(0);
            // Xây dựng thanh thumbnail
            buildThumbnailBar();

            Object idObj = selectedAuction.get("id");
            this.currentAuctionId = (idObj instanceof Number) ? ((Number) idObj).intValue() : -1;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Gallery ảnh sản phẩm
    // -------------------------------------------------------------------------

    /** Hiển thị ảnh tại vị trí index lên imgProduct chính */
    private void showImageAt(int index) {
        if (productImageUrls.isEmpty()) return;
        if (index < 0 || index >= productImageUrls.size()) return;
        currentImageIndex = index;
        String url = productImageUrls.get(index);
        try {
            if (url.startsWith("http")) {
                imgProduct.setImage(new Image(url, true));
            }
        } catch (Exception e) {
            System.err.println("Lỗi tải ảnh (Selling gallery): " + e.getMessage());
        }
        // Làm nổi bật thumbnail đang chọn
        highlightThumbnail(index);
    }

    /** Xây dựng thanh thumbnail bên dưới ảnh chính */
    private void buildThumbnailBar() {
        if (hboxThumbnails == null) return;
        hboxThumbnails.getChildren().clear();

        // Không hiện thumbnail nếu chỉ có 1 ảnh
        if (productImageUrls.size() <= 1) return;

        for (int i = 0; i < productImageUrls.size(); i++) {
            final int idx = i;
            String url = productImageUrls.get(i);

            // ImageView fill cứng 90×70, không giữ tỉ lệ → ảnh nào cũng đầy khung
            ImageView thumb = new ImageView();
            thumb.setFitWidth(90);
            thumb.setFitHeight(70);
            thumb.setPreserveRatio(false);
            thumb.setSmooth(true);
            try {
                if (url.startsWith("http")) {
                    thumb.setImage(new Image(url, 90, 70, false, true, true));
                }
            } catch (Exception ignored) {}

            // Clip bo góc
            Rectangle clip = new Rectangle(90, 70);
            clip.setArcWidth(8);
            clip.setArcHeight(8);
            thumb.setClip(clip);

            StackPane cell = new StackPane(thumb);
            cell.setPrefSize(96, 76);
            cell.setMinSize(96, 76);
            cell.setMaxSize(96, 76);
            cell.setStyle(idx == currentImageIndex
                    ? "-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-padding: 3; -fx-cursor: hand;"
                    : "-fx-background-color: #3a3a3a; -fx-background-radius: 10; -fx-padding: 3; -fx-cursor: hand;");

            cell.setOnMouseClicked(e -> showImageAt(idx));
            cell.setId("thumb_" + i);
            hboxThumbnails.getChildren().add(cell);
        }
    }

    /** Làm nổi bật (viền trắng) thumbnail tại index đang chọn */
    private void highlightThumbnail(int activeIndex) {
        if (hboxThumbnails == null) return;
        for (int i = 0; i < hboxThumbnails.getChildren().size(); i++) {
            hboxThumbnails.getChildren().get(i).setStyle(i == activeIndex
                    ? "-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-padding: 3; -fx-cursor: hand;"
                    : "-fx-background-color: #444; -fx-background-radius: 8; -fx-padding: 3; -fx-cursor: hand;");
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
        if (currentAuctionId == -1) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Lịch sử đấu giá");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color: #2a2a2a;");

        VBox container = new VBox(12);
        container.setStyle("-fx-padding: 20; -fx-background-color: #2a2a2a;");
        container.setPrefWidth(480);
        Label title = new Label("LỊCH SỬ ĐẤU GIÁ");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #555;");

        VBox listBox = new VBox(8);
        listBox.setStyle("-fx-padding: 4 0 0 0;");

        dialogHistoryBox = listBox; // GẮN CẦU NỐI CHO OBSERVER
        listBox.getChildren().add(new Label("⏳ Đang tải dữ liệu từ máy chủ..."));

        ScrollPane scrollPane = new ScrollPane(listBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(350);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #2a2a2a; -fx-border-color: transparent;");

        container.getChildren().addAll(title, sep, scrollPane);
        dialog.getDialogPane().setContent(container);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE)
                .setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        // Dọn dẹp cầu nối khi người dùng tắt cửa sổ
        dialog.setOnHidden(e -> dialogHistoryBox = null);

        // Kích hoạt nạp dữ liệu lần đầu (chạy ngầm để không đơ màn hình)
        new Thread(this::loadBidHistory).start();

        dialog.showAndWait();
    }
    // -------------------------------------------------------------------------
    // Biểu đồ giá
    // -------------------------------------------------------------------------

    @FXML
    private void handleShowPriceChart() {
        if (currentAuctionId == -1) return;

        javafx.scene.chart.NumberAxis xAxis = new javafx.scene.chart.NumberAxis();
        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        xAxis.setLabel("Lượt đặt giá");
        yAxis.setLabel("Giá (VNĐ)");
        xAxis.setTickUnit(1);
        xAxis.setMinorTickVisible(false);
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);

        dialogXAxis = xAxis; // GẮN CẦU NỐI CHO OBSERVER
        dialogYAxis = yAxis;

        javafx.scene.chart.LineChart<Number, Number> lineChart = new javafx.scene.chart.LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Biến động giá đấu giá");
        lineChart.setAnimated(false); // Phải tắt Animated để Observer vẽ mượt
        lineChart.setLegendVisible(false);
        lineChart.setPrefSize(720, 430);
        lineChart.setStyle("-fx-background-color: #1e1e1e;");

        javafx.scene.chart.XYChart.Series<Number, Number> series = new javafx.scene.chart.XYChart.Series<>();
        series.setName("Giá đặt");
        lineChart.getData().add(series);

        dialogChartSeries = series; // GẮN CẦU NỐI CHO OBSERVER

        lineChart.getStylesheets().add(
                "data:text/css," +
                        ".chart-plot-background{-fx-background-color:#2a2a2a;}" +
                        ".chart-title{-fx-text-fill:#ffffff;-fx-font-size:15px;-fx-font-weight:bold;}" +
                        ".axis-label{-fx-text-fill:#dddddd;-fx-font-size:12px;}" +
                        ".axis{-fx-tick-label-fill:#cccccc;}" +
                        ".default-color0.chart-series-line{-fx-stroke:#4FC3F7;-fx-stroke-width:2.5px;}" +
                        ".default-color0.chart-line-symbol{-fx-background-color:transparent;}" +
                        ".chart-vertical-grid-lines{-fx-stroke:#3a3a3a;}" +
                        ".chart-horizontal-grid-lines{-fx-stroke:#3a3a3a;}"
        );

        javafx.scene.layout.HBox chartWrapper = new javafx.scene.layout.HBox(lineChart);
        chartWrapper.setAlignment(javafx.geometry.Pos.CENTER);
        javafx.scene.layout.HBox.setHgrow(lineChart, javafx.scene.layout.Priority.ALWAYS);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Biểu đồ giá");
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

        // Dọn dẹp cầu nối khi đóng
        dialog.setOnHidden(e -> {
            dialogChartSeries = null;
            dialogXAxis = null;
            dialogYAxis = null;
        });

        // Kích hoạt nạp dữ liệu lần đầu
        new Thread(this::loadBidHistory).start();

        dialog.showAndWait();
    }
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

    // 1. Thêm từ khóa synchronized để các luồng xếp hàng gọi mạng, không đè gói tin lên nhau
    private synchronized void loadBidHistory() {
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

        // 2. Ép buộc sắp xếp lịch sử từ thấp đến cao (Chống loạn thứ tự từ Database)
        if (history != null && !history.isEmpty()) {
            history.sort((b1, b2) -> Double.compare(
                    ((Number) b1.get("amount")).doubleValue(),
                    ((Number) b2.get("amount")).doubleValue()
            ));
        }

        // Cập nhật UI trên JavaFX thread
        Platform.runLater(() -> {

            // --- ⚠️ VAN MỘT CHIỀU CHỐNG GIẬT LÙI THỜI GIAN ⚠️ ---
            int newSize = (history != null) ? history.size() : 0;
            // Nếu số lượng lượt đặt giá mới nhỏ hơn hoặc bằng số lượng đang hiển thị ngoài màn hình chính
            // (vboxBidHistory.getChildren().size()), chứng tỏ đây là gói tin cũ kẹt mạng -> VỨT BỎ NGAY!
            if (vboxBidHistory != null && newSize <= vboxBidHistory.getChildren().size() && vboxBidHistory.getChildren().size() > 1) {
                return;
            }

            // --- 0. CẬP NHẬT NHÃN GIÁ MÀU ĐỎ MỚI NHẤT ---
            double highestBid = currentHighestBid;
            if (history != null && !history.isEmpty()) {
                highestBid = ((Number) history.get(history.size() - 1).get("amount")).doubleValue();
            }

            if (highestBid > currentHighestBid) {
                currentHighestBid = highestBid;
                lblCurrentPrice.setText(String.format("%,.0f VNĐ", highestBid));
            }

            // --- 1. CẬP NHẬT MAIN UI (Có giáp thép chống Crash) ---
            if (vboxBidHistory != null) {
                vboxBidHistory.getChildren().clear();
                if (history == null || history.isEmpty()) {
                    vboxBidHistory.getChildren().add(new Label("Chưa có lịch sử đặt giá."));
                } else {
                    for (int i = history.size() - 1; i >= 0; i--) {
                        Map<String, Object> bid = history.get(i);
                        double amount      = ((Number) bid.get("amount")).doubleValue();
                        String bidTime     = String.valueOf(bid.get("bidTime"));
                        String bidderName  = bid.get("bidderName") != null ? String.valueOf(bid.get("bidderName")) : "Bidder";

                        Label lbl = new Label(String.format("%s  —  %,.0fVND  —  %s", bidderName, amount, bidTime));
                        lbl.setStyle("-fx-font-size: 13px; -fx-padding: 4 0; -fx-text-fill: #cccccc;");
                        vboxBidHistory.getChildren().add(lbl);
                    }
                }
            }

            // --- 2. CẬP NHẬT DIALOG LỊCH SỬ (Nếu người dùng đang mở) ---
            if (dialogHistoryBox != null) {
                dialogHistoryBox.getChildren().clear();
                if (history == null || history.isEmpty()) {
                    Label empty = new Label("Chưa có lịch sử đặt giá.");
                    empty.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 14px;");
                    dialogHistoryBox.getChildren().add(empty);
                } else {
                    for (int i = history.size() - 1; i >= 0; i--) {
                        Map<String, Object> bid = history.get(i);
                        double amount     = ((Number) bid.get("amount")).doubleValue();
                        String bidTime    = String.valueOf(bid.get("bidTime"));
                        String bidderName = bid.get("bidderName") != null ? String.valueOf(bid.get("bidderName")) : "Bidder";

                        Label row = new Label(String.format("👤 %s   —   %,.0f VNĐ   —   %s", bidderName, amount, bidTime));
                        row.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 14px; -fx-padding: 6 10; -fx-background-color: #1e1e1e; -fx-background-radius: 6;");
                        row.setMaxWidth(Double.MAX_VALUE);
                        dialogHistoryBox.getChildren().add(row);
                    }
                }
            }

            // --- 3. CẬP NHẬT DIALOG BIỂU ĐỒ (Nếu người dùng đang mở) ---
            if (dialogChartSeries != null && dialogXAxis != null && dialogYAxis != null) {
                dialogChartSeries.getData().clear();
                double maxPrice = currentHighestBid;
                int dataCount = (history != null && !history.isEmpty()) ? history.size() : 1;

                if (history != null && !history.isEmpty()) {
                    for (int i = 0; i < history.size(); i++) {
                        Map<String, Object> bid = history.get(i);
                        double amount = ((Number) bid.get("amount")).doubleValue();
                        if (amount > maxPrice) maxPrice = amount;

                        javafx.scene.chart.XYChart.Data<Number, Number> dp = new javafx.scene.chart.XYChart.Data<>(i + 1, amount);
                        dp.setNode(makeDotNode(amount));
                        dialogChartSeries.getData().add(dp);
                    }
                } else {
                    javafx.scene.chart.XYChart.Data<Number, Number> dp = new javafx.scene.chart.XYChart.Data<>(1, currentHighestBid);
                    dp.setNode(makeDotNode(currentHighestBid));
                    dialogChartSeries.getData().add(dp);
                }

                // Tự động thu phóng trục toạ độ
                dialogXAxis.setUpperBound(dataCount + 1.5);
                dialogXAxis.setTickUnit(Math.max(1, dataCount / 10.0));
                dialogYAxis.setUpperBound(maxPrice * 1.15);
                dialogYAxis.setTickUnit(maxPrice * 1.15 / 8);
            }
        });
    }
    private void startListeningForPrices() {
        Thread listenerThread = new Thread(() -> {
            try {
                radioSocket = new java.net.Socket("localhost", 9999);
                java.io.PrintWriter out = new java.io.PrintWriter(radioSocket.getOutputStream(), true);
                java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(radioSocket.getInputStream()));

                out.println("{\"action\": \"SUBSCRIBE_PRICE\"}");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.contains("UPDATE_PRICE")) {
                        Map<String, Object> data = gson.fromJson(message, new TypeToken<Map<String, Object>>(){}.getType());
                        int id = ((Number) data.get("auctionId")).intValue();
                        double newPrice = ((Number) data.get("newPrice")).doubleValue();
                        String newEndTime = data.get("newEndTime") != null ? String.valueOf(data.get("newEndTime")) : null;

                        if (id == currentAuctionId) {
                            // 1. NHẢY GIÁ ĐỎ + CẬP NHẬT THỜI GIAN KẾT THÚC LẬP TỨC TRÊN GIAO DIỆN
                            Platform.runLater(() -> {
                                if (newPrice > currentHighestBid) {
                                    currentHighestBid = newPrice;
                                    lblCurrentPrice.setText(String.format("%,.0f VNĐ", newPrice));
                                }
                                // Observer cập nhật endTime mỗi khi anti-sniping gia hạn
                                if (newEndTime != null && !newEndTime.isEmpty()) {
                                    lblEndTime.setText(newEndTime);
                                }
                            });

                            // 2. KÉO LỊCH SỬ Ở LUỒNG NGẦM (Không làm đơ màn hình)
                            loadBidHistory();
                        }

                    } else if (message.contains("NEW_CHAT_MESSAGE")) {
                        Map<String, Object> data = gson.fromJson(message, new TypeToken<Map<String, Object>>(){}.getType());
                        int id           = ((Number) data.get("auctionId")).intValue();
                        String sender    = String.valueOf(data.getOrDefault("sender", "Ẩn danh"));
                        String content   = String.valueOf(data.getOrDefault("content", ""));

                        if (id == currentAuctionId) {
                            String myName = UserSession.getInstance().getDisplayName();
                            boolean isMine = sender.equals(myName);
                            Platform.runLater(() -> appendChatMessage(sender, content, isMine));
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("📻 Đài phát thanh đã ngắt kết nối.");
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Tải lịch sử chat từ server và hiển thị lại toàn bộ.
     * Gọi lần đầu khi initialize và có thể gọi lại sau khi reconnect.
     */
    private void loadChatHistory() {
        if (currentAuctionId == -1) return;

        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", currentAuctionId);
        Response response = ServerConnection.getInstance().send("GET_CHAT_HISTORY", data);

        if (!response.isSuccess()) return;

        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> history;
        try {
            history = new Gson().fromJson(response.getData(), listType);
        } catch (Exception e) {
            return;
        }
        if (history == null || history.isEmpty()) return;

        String myName = UserSession.getInstance().getDisplayName();

        Platform.runLater(() -> {
            if (vboxChatMessages == null) return;
            vboxChatMessages.getChildren().clear();
            for (Map<String, Object> msg : history) {
                String sender  = String.valueOf(msg.getOrDefault("sender",  "Ẩn danh"));
                String content = String.valueOf(msg.getOrDefault("content", ""));
                appendChatMessage(sender, content, sender.equals(myName));
            }
        });
    }

    private void stopListening() {
        try {
            if (radioSocket != null && !radioSocket.isClosed()) radioSocket.close();
        } catch (Exception ignored) {}
    }

    // -------------------------------------------------------------------------
    // Live Chat
    // -------------------------------------------------------------------------

    /** Gửi tin nhắn chat lên server */
    @FXML
    private void handleSendChat() {
        if (txtChatInput == null) return;
        String content = txtChatInput.getText().trim();
        if (content.isEmpty()) return;
        if (currentAuctionId == -1) return;

        txtChatInput.clear();

        // KHÔNG appendChatMessage local ở đây.
        // Server sẽ broadcast lại cho tất cả (kể cả mình) → appendChatMessage sẽ được gọi
        // khi nhận broadcast, tránh tin nhắn hiện 2 lần.
        new Thread(() -> {
            Map<String, Object> data = new HashMap<>();
            data.put("auctionId",  currentAuctionId);
            data.put("senderName", UserSession.getInstance().getDisplayName());
            data.put("content",    content);
            ServerConnection.getInstance().send("SEND_CHAT", data);
        }).start();
    }

    /**
     * Thêm một bong bóng tin nhắn vào khung chat.
     * @param sender  Tên người gửi
     * @param content Nội dung tin nhắn
     * @param isMine  true = tin của chính mình (căn phải, màu xanh)
     */
    private void appendChatMessage(String sender, String content, boolean isMine) {
        if (vboxChatMessages == null) return;

        // Label tên người gửi
        Label lblSender = new Label(sender);
        lblSender.setStyle("-fx-text-fill: " + (isMine ? "#4FC3F7" : "#FFD54F") + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        // Label nội dung
        Label lblContent = new Label(content);
        lblContent.setWrapText(true);
        lblContent.setMaxWidth(300);
        lblContent.setStyle(
                "-fx-text-fill: white; -fx-font-size: 13px;" +
                "-fx-background-color: " + (isMine ? "#1565C0" : "#37474F") + ";" +
                "-fx-background-radius: 10; -fx-padding: 8 12;"
        );

        // Bong bóng gồm tên + nội dung
        VBox bubble = new VBox(2, lblSender, lblContent);
        bubble.setMaxWidth(320);

        // HBox — tất cả tin nhắn căn trái
        HBox row = new HBox(bubble);
        row.setPadding(new Insets(3, 8, 3, 8));
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        vboxChatMessages.getChildren().add(row);

        // Tự cuộn xuống cuối
        if (scrollPaneChat != null) {
            scrollPaneChat.layout();
            scrollPaneChat.setVvalue(1.0);
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
        stopListening();
        Main.changeScene("/view/login.fxml");
    }

    @FXML
    private void goHome() {
        stopListening();
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
