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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HomeController {

    @FXML private Label lblUsername;
    @FXML private VBox vboxProducts;

    private final Gson gson = new Gson();

    private java.net.Socket radioSocket;

    @FXML
    public void initialize() {
        if (UserSession.getInstance().isLoggedIn()) {
            AppData.username = UserSession.getInstance().getDisplayName();
            lblUsername.setText(AppData.username);
        }
        loadAuctionsFromServer();

        startListeningForPrices();
    }

    /*private void loadAuctionsFromServer() {
        Response response = ServerConnection.getInstance().send("GET_AUCTIONS", null);

        if (!response.isSuccess()) {
            showError("Không thể tải danh sách đấu giá: " + response.getMessage());
            return;
        }

        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> auctions = null;
        try {
            Object rawData = response.getData();
            if (rawData instanceof String) {
                auctions = gson.fromJson((String) rawData, listType);
            } else if (rawData instanceof List) {
                auctions = (List<Map<String, Object>>) rawData;
            } else {
                auctions = gson.fromJson(gson.toJson(rawData), listType);
            }
        } catch (Exception e) {
            showError("Lỗi đọc dữ liệu từ Server!");
            e.printStackTrace();
            return;
        }

        if (auctions == null || auctions.isEmpty()) {
            Platform.runLater(() -> {
                vboxProducts.getChildren().clear();
                Label lbl = new Label("Hiện chưa có phiên đấu giá nào.");
                lbl.setStyle("-fx-text-fill: #888; -fx-font-size: 14px; -fx-padding: 20;");
                vboxProducts.getChildren().add(lbl);
            });
            return;
        }

        final List<Map<String, Object>> finalAuctions = auctions;
        Platform.runLater(() -> {
            vboxProducts.getChildren().clear();

            // Tách thành 2 nhóm và sắp xếp theo endTime mới nhất lên đầu
            List<Map<String, Object>> running = finalAuctions.stream()
                    .filter(a -> "RUNNING".equals(a.get("status")))
                    .sorted((a, b) -> String.valueOf(b.getOrDefault("endTime", ""))
                            .compareTo(String.valueOf(a.getOrDefault("endTime", ""))))
                    .collect(java.util.stream.Collectors.toList());

            List<Map<String, Object>> finished = finalAuctions.stream()
                    .filter(a -> "FINISHED".equals(a.get("status")))
                    .sorted((a, b) -> String.valueOf(b.getOrDefault("endTime", ""))
                            .compareTo(String.valueOf(a.getOrDefault("endTime", ""))))
                    .collect(java.util.stream.Collectors.toList());

            // Section RUNNING
            if (!running.isEmpty()) {
                Label runningHeader = new Label("🟢 Đang diễn ra (" + running.size() + ")");
                runningHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2e7d32; -fx-padding: 8 0 4 4;");
                vboxProducts.getChildren().add(runningHeader);
                for (Map<String, Object> auction : running) {
                    vboxProducts.getChildren().add(createAuctionItem(auction));
                }
            }

            // Section FINISHED
            if (!finished.isEmpty()) {
                Label finishedHeader = new Label("🔴 Đã kết thúc (" + finished.size() + ")");
                finishedHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #888; -fx-padding: 16 0 4 4;");
                vboxProducts.getChildren().add(finishedHeader);
                for (Map<String, Object> auction : finished) {
                    vboxProducts.getChildren().add(createAuctionItem(auction));
                }
            }

            if (running.isEmpty() && finished.isEmpty()) {
                Label lbl = new Label("Hiện chưa có phiên đấu giá nào.");
                lbl.setStyle("-fx-text-fill: #888; -fx-font-size: 14px; -fx-padding: 20;");
                vboxProducts.getChildren().add(lbl);
            }
        });
    }*/

    private VBox createAuctionItem(Map<String, Object> auction) {
        VBox itemBox = new VBox();
        // Đặt chiều rộng cố định khoảng 300px để 1 hàng chứa vừa 3 ô
        itemBox.setPrefWidth(300);
        itemBox.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #333; -fx-cursor: hand;");

        // --- Xử lý Hình ảnh (Cho nằm trên cùng) ---
        ImageView imgView = new ImageView();
        String imgPath = (String) auction.get("itemImagePath");
        try {
            if (imgPath != null && !imgPath.trim().isEmpty()) {
                if (imgPath.length() > 500) {
                    byte[] imageBytes = Base64.getDecoder().decode(imgPath);
                    imgView.setImage(new Image(new ByteArrayInputStream(imageBytes)));
                } else if (imgPath.startsWith("http") || imgPath.startsWith("file:")) {
                    imgView.setImage(new Image(imgPath));
                } else {
                    java.io.InputStream imageStream = getClass().getResourceAsStream(imgPath);
                    if (imageStream != null) imgView.setImage(new Image(imageStream));
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi giải mã ảnh (Home): " + e.getMessage());
        }
        // Căn chỉnh ảnh to ra choán hết chiều rộng của thẻ
        imgView.setFitWidth(300);
        imgView.setFitHeight(180);
        // Bạn có thể cân nhắc tắt PreserveRatio nếu muốn ảnh lấp đầy khung, hoặc bật nếu sợ méo ảnh

        // --- Xử lý Thông tin chữ (Nằm dưới ảnh) ---
        VBox infoBox = new VBox(10); // Khoảng cách giữa các dòng chữ
        infoBox.setStyle("-fx-padding: 15;"); // Lề thụt vào cho phần chữ

        int auctionId   = auction.get("id") != null ? ((Number) auction.get("id")).intValue() : 0;
        String itemName = (String) auction.getOrDefault("name", "Sản phẩm #" + auctionId);
        String desc     = (String) auction.getOrDefault("description", "");
        String status   = (String) auction.getOrDefault("status", "UNKNOWN");
        double bid      = auction.get("startingPrice") != null ? ((Number) auction.get("startingPrice")).doubleValue() : 0;
        String endTime  = (String) auction.getOrDefault("endTime", "");

        Label nameLabel = new Label(itemName);
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        nameLabel.setWrapText(true); // Tự động xuống dòng nếu tên quá dài

        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 13px;");
        descLabel.setWrapText(true);

        Label priceLabel = new Label(String.format("Giá: %,.0f VND", bid));
        priceLabel.setStyle("-fx-text-fill: #ff5252; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label statusLabel = new Label("Trạng thái: " + status + "\nKết thúc: " + endTime);
        String statusColor = "RUNNING".equals(status) ? "#4CAF50" : "#888";
        statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 13px;");

        infoBox.getChildren().addAll(nameLabel, descLabel, priceLabel, statusLabel);

        // Ghép ảnh và chữ vào thẻ
        itemBox.getChildren().addAll(imgView, infoBox);

        // Hiệu ứng Hover đổi màu nền xám sáng hơn
        itemBox.setOnMouseEntered(e -> itemBox.setStyle("-fx-background-color: #383838; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #555; -fx-cursor: hand;"));
        itemBox.setOnMouseExited(e  -> itemBox.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #333; -fx-cursor: hand;"));

        itemBox.setOnMouseClicked(e -> {
            AppData.selectedAuction = auction;
            stopListening();
            Main.changeScene("/view/selling_product.fxml");
        });

        return itemBox;
    }

    private void loadAuctionsFromServer() {
        Response response = ServerConnection.getInstance().send("GET_AUCTIONS", null);

        if (!response.isSuccess()) {
            showError("Không thể tải danh sách đấu giá: " + response.getMessage());
            return;
        }

        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> auctions = null;
        try {
            Object rawData = response.getData();
            if (rawData instanceof String) {
                auctions = gson.fromJson((String) rawData, listType);
            } else if (rawData instanceof List) {
                auctions = (List<Map<String, Object>>) rawData;
            } else {
                auctions = gson.fromJson(gson.toJson(rawData), listType);
            }
        } catch (Exception e) {
            showError("Lỗi đọc dữ liệu từ Server!");
            e.printStackTrace();
            return;
        }

        if (auctions == null || auctions.isEmpty()) {
            Platform.runLater(() -> {
                vboxProducts.getChildren().clear();
                Label lbl = new Label("Hiện chưa có phiên đấu giá nào.");
                lbl.setStyle("-fx-text-fill: #888; -fx-font-size: 18px; -fx-padding: 20;");
                vboxProducts.getChildren().add(lbl);
            });
            return;
        }

        final List<Map<String, Object>> finalAuctions = auctions;
        Platform.runLater(() -> {
            vboxProducts.getChildren().clear();

            // TĂNG LỀ 2 BÊN RỘNG RA (Top: 30, Right: 150, Bottom: 50, Left: 150)

            // Tách thành 2 nhóm và sắp xếp theo endTime mới nhất lên đầu
            List<Map<String, Object>> running = finalAuctions.stream()
                    .filter(a -> "RUNNING".equals(a.get("status")))
                    .sorted((a, b) -> String.valueOf(b.getOrDefault("endTime", ""))
                            .compareTo(String.valueOf(a.getOrDefault("endTime", ""))))
                    .collect(java.util.stream.Collectors.toList());

            List<Map<String, Object>> finished = finalAuctions.stream()
                    .filter(a -> "FINISHED".equals(a.get("status")))
                    .sorted((a, b) -> String.valueOf(b.getOrDefault("endTime", ""))
                            .compareTo(String.valueOf(a.getOrDefault("endTime", ""))))
                    .collect(java.util.stream.Collectors.toList());

            // Section RUNNING
            if (!running.isEmpty()) {
                Label runningHeader = new Label("🟢 Đang diễn ra (" + running.size() + ")");
                // Chỉnh lại cỡ chữ to hơn, màu xanh sáng nổi bật trên nền đen
                runningHeader.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #4CAF50; -fx-padding: 0 0 20 0;");
                vboxProducts.getChildren().add(runningHeader);

                // SỬ DỤNG FLOWPANE ĐỂ DÀN HÀNG NGANG
                FlowPane runningFlow = new FlowPane();
                runningFlow.setHgap(40); // Khoảng cách chiều ngang giữa các thẻ sản phẩm
                runningFlow.setVgap(40); // Khoảng cách chiều dọc giữa các thẻ sản phẩm

                for (Map<String, Object> auction : running) {
                    runningFlow.getChildren().add(createAuctionItem(auction));
                }
                vboxProducts.getChildren().add(runningFlow);
            }

            // Section FINISHED
            if (!finished.isEmpty()) {
                Label finishedHeader = new Label("🔴 Đã kết thúc (" + finished.size() + ")");
                // Chỉnh lại cỡ chữ to hơn, màu xám sáng
                finishedHeader.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #aaaaaa; -fx-padding: 40 0 20 0;");
                vboxProducts.getChildren().add(finishedHeader);

                // SỬ DỤNG FLOWPANE ĐỂ DÀN HÀNG NGANG
                FlowPane finishedFlow = new FlowPane();
                finishedFlow.setHgap(40); // Khoảng cách chiều ngang
                finishedFlow.setVgap(40); // Khoảng cách chiều dọc

                for (Map<String, Object> auction : finished) {
                    finishedFlow.getChildren().add(createAuctionItem(auction));
                }
                vboxProducts.getChildren().add(finishedFlow);
            }

            if (running.isEmpty() && finished.isEmpty()) {
                Label lbl = new Label("Hiện chưa có phiên đấu giá nào.");
                lbl.setStyle("-fx-text-fill: #888; -fx-font-size: 18px; -fx-padding: 20;");
                vboxProducts.getChildren().add(lbl);
            }
        });
    }



    private void showMyAuctionResults() {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", UserSession.getInstance().getUserId());
        Response response = ServerConnection.getInstance().send("GET_MY_AUCTION_RESULTS", data);

        // Tạo dialog hiển thị kết quả
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Kết quả đấu giá của tôi");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(500);

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 15;");

        if (!response.isSuccess()) {
            content.getChildren().add(new Label("Không thể tải kết quả: " + response.getMessage()));
        } else {
            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> results;
            try {
                results = gson.fromJson(response.getData(), listType);
            } catch (Exception e) {
                results = null;
            }

            if (results == null || results.isEmpty()) {
                content.getChildren().add(new Label("Bạn chưa tham gia phiên đấu giá nào đã kết thúc."));
            } else {
                for (Map<String, Object> r : results) {
                    String itemName   = String.valueOf(r.getOrDefault("itemName", "?"));
                    double finalPrice = ((Number) r.get("finalPrice")).doubleValue();
                    boolean isWinner  = Boolean.TRUE.equals(r.get("isWinner"));
                    boolean isSeller  = Boolean.TRUE.equals(r.get("isSeller"));
                    String endTime    = String.valueOf(r.getOrDefault("endTime", ""));

                    String role   = isSeller ? "Người bán" : (isWinner ? "🏆 Người thắng" : "Người tham gia");
                    String color  = isWinner ? "#28a745" : (isSeller ? "#2196F3" : "#888");

                    Label lbl = new Label(String.format("%s  |  %s  |  Giá cuối: %,.0fVND  |  %s",
                            role, itemName, finalPrice, endTime));
                    lbl.setStyle("-fx-font-size: 13px; -fx-padding: 8; -fx-background-color: #f9f9f9; "
                            + "-fx-background-radius: 5; -fx-text-fill: " + color + ";");
                    lbl.setWrapText(true);
                    content.getChildren().add(lbl);
                }
            }
        }

        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setPrefHeight(400);
        dialog.getDialogPane().setContent(sp);
        dialog.showAndWait();
    }
    private void startListeningForPrices() {
        Thread listenerThread = new Thread(() -> {
            try {
                radioSocket = new java.net.Socket("10.11.200.251", 9999);
                java.io.PrintWriter out = new java.io.PrintWriter(radioSocket.getOutputStream(), true);
                java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(radioSocket.getInputStream()));

                out.println("{\"action\": \"SUBSCRIBE_PRICE\"}");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.contains("UPDATE_PRICE")) {
                        Map<String, Object> data = gson.fromJson(message, new TypeToken<Map<String, Object>>(){}.getType());
                        int id = ((Number) data.get("auctionId")).intValue();
                        double newPrice = ((Number) data.get("newPrice")).doubleValue();

                        Platform.runLater(() -> {
                            // Cập nhật lại giao diện (cần quét lại các VBox để tìm đúng Label)
                            loadAuctionsFromServer(); // Tạm thời load lại cho an toàn nếu chưa lưu Map
                        });
                    }
                }
            } catch (Exception e) {
                System.out.println("Đài phát thanh ngầm đã đóng.");
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void stopListening() {
        try {
            if (radioSocket != null && !radioSocket.isClosed()) radioSocket.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showError(String msg) {
        Platform.runLater(() -> {
            Label lbl = new Label(msg);
            lbl.setStyle("-fx-text-fill: #888; -fx-font-size: 14px; -fx-padding: 20;");
            vboxProducts.getChildren().clear();
            vboxProducts.getChildren().add(lbl);
        });
    }

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
    private void goToMyResults() {
        stopListening();
        showMyAuctionResults();
    }

    @FXML
    private void addNewProduct() {
        com.auction.client.Main.changeScene("/view/add_product.fxml");
    }

    @FXML
    private void logout() {
        stopListening();
        UserSession.getInstance().logout();
        Main.changeScene("/view/login.fxml");
    }
}
