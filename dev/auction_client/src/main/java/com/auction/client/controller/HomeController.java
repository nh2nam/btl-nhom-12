package com.auction.client.controller;

import com.auction.client.Main;
import com.auction.client.model.AppData;
import com.auction.client.network.Response;
import com.auction.client.network.ServerConnection;
import com.auction.client.session.UserSession;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Đã dọn dẹp các thư viện Base64 và ByteArrayInputStream không còn dùng tới

public class HomeController {

    @FXML private Label lblUsername;
    @FXML private VBox vboxProducts;

    private final Gson gson = new Gson();

    private java.net.Socket radioSocket;
    private List<Map<String, Object>> allAuctions = new ArrayList<>();
    private String categoryFilter;
    private String statusFilter;

    private static final Map<String, String> MENU_TO_CATEGORY = Map.of(
            "Nghệ thuật", "Arts",
            "Điện tử", "Electronics",
            "Bất động sản", "Real estate",
            "Thời trang", "Fashion",
            "Tài sản khác", "Other"
    );

    private static final Map<String, String> MENU_TO_STATUS = Map.of(
            "Phiên đấu giá đang diễn ra", "RUNNING",
            "Phiên đấu giá đã kết thúc", "FINISHED"
    );

    @FXML
    public void initialize() {
        if (UserSession.getInstance().isLoggedIn()) {
            AppData.username = UserSession.getInstance().getDisplayName();
            lblUsername.setText(AppData.username);
        }
        loadAuctionsFromServer();
        startListeningForPrices();
    }

    private VBox createAuctionItem(Map<String, Object> auction) {
        VBox itemBox = new VBox();
        itemBox.setPrefWidth(300);
        itemBox.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #333; -fx-cursor: hand;");

        // --- Xử lý Hình ảnh (Đã nâng cấp dùng Link Cloudinary) ---
        ImageView imgView = new ImageView();
        String imgPath = (String) auction.get("itemImagePath");
        try {
            if (imgPath != null && !imgPath.trim().isEmpty() && imgPath.startsWith("http")) {
                // Tham số 'true' giúp tải ảnh ngầm, không làm đơ giao diện JavaFX
                imgView.setImage(new Image(imgPath, true));
            } else {
                // Tùy chọn: Bạn có thể set một ảnh mặc định (Placeholder) ở đây nếu SP chưa có ảnh
                // imgView.setImage(new Image(getClass().getResourceAsStream("/images/default.png")));
            }
        } catch (Exception e) {
            System.err.println("Lỗi tải ảnh từ mạng (Home): " + e.getMessage());
        }

        imgView.setFitWidth(300);
        imgView.setFitHeight(180);

        // --- Xử lý Thông tin chữ ---
        VBox infoBox = new VBox(10);
        infoBox.setStyle("-fx-padding: 15;");

        int auctionId   = auction.get("id") != null ? ((Number) auction.get("id")).intValue() : 0;
        String itemName = (String) auction.getOrDefault("name", "Sản phẩm #" + auctionId);
        String desc     = (String) auction.getOrDefault("description", "");
        String status   = (String) auction.getOrDefault("status", "UNKNOWN");
        Object currentBidObj = auction.get("currentHighestBid");
        double bid = (currentBidObj instanceof Number) ? ((Number) currentBidObj).doubleValue()
                : (auction.get("startingPrice") instanceof Number ? ((Number) auction.get("startingPrice")).doubleValue() : 0);
        String endTime  = (String) auction.getOrDefault("endTime", "");

        Label nameLabel = new Label(itemName);
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        nameLabel.setWrapText(true);

        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 13px;");
        descLabel.setWrapText(true);

        Label priceLabel = new Label(String.format("Giá: %,.0f VND", bid));
        priceLabel.setStyle("-fx-text-fill: #ff5252; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label statusLabel = new Label("Trạng thái: " + status + "\nKết thúc: " + endTime);
        String statusColor = "RUNNING".equals(status) ? "#4CAF50" : "#888";
        statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 13px;");

        infoBox.getChildren().addAll(nameLabel, descLabel, priceLabel, statusLabel);

        itemBox.getChildren().addAll(imgView, infoBox);

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

        allAuctions = (auctions != null) ? auctions : new ArrayList<>();
        Platform.runLater(this::renderAuctionList);
    }

    @FXML
    private void filterByCategory(ActionEvent event) {
        MenuItem item = (MenuItem) event.getSource();
        categoryFilter = MENU_TO_CATEGORY.get(item.getText());
        auctionMenu.hide();
        renderAuctionList();
    }

    @FXML
    private void filterByStatus(ActionEvent event) {
        MenuItem item = (MenuItem) event.getSource();
        String menuText = item.getText();
        if ("Tất cả phiên đấu giá".equals(menuText)) {
            statusFilter = null;
            loadAuctionsFromServer();
        } else {
            statusFilter = MENU_TO_STATUS.get(menuText);
            renderAuctionList();
        }
        sessionMenu.hide();
    }

    private boolean matchesCategoryFilter(Map<String, Object> auction) {
        if (categoryFilter == null) return true;
        String category = String.valueOf(auction.getOrDefault("category", ""));
        return categoryFilter.equalsIgnoreCase(category);
    }

    private boolean matchesStatusFilter(Map<String, Object> auction) {
        if (statusFilter == null) return true;
        return statusFilter.equals(auction.get("status"));
    }

    private void renderAuctionList() {
        vboxProducts.getChildren().clear();

        List<Map<String, Object>> filtered = allAuctions.stream()
                .filter(a -> matchesCategoryFilter(a) && matchesStatusFilter(a))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Label lbl = new Label(buildEmptyFilterMessage());
            lbl.setStyle("-fx-text-fill: #888; -fx-font-size: 18px; -fx-padding: 20;");
            vboxProducts.getChildren().add(lbl);
            return;
        }

        List<Map<String, Object>> running = filtered.stream()
                .filter(a -> "RUNNING".equals(a.get("status")))
                .sorted((a, b) -> String.valueOf(b.getOrDefault("endTime", ""))
                        .compareTo(String.valueOf(a.getOrDefault("endTime", ""))))
                .collect(Collectors.toList());

        List<Map<String, Object>> finished = filtered.stream()
                .filter(a -> "FINISHED".equals(a.get("status")))
                .sorted((a, b) -> String.valueOf(b.getOrDefault("endTime", ""))
                        .compareTo(String.valueOf(a.getOrDefault("endTime", ""))))
                .collect(Collectors.toList());

        if (!running.isEmpty()) {
            Label runningHeader = new Label("🟢 Đang diễn ra (" + running.size() + ")");
            runningHeader.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #4CAF50; -fx-padding: 0 0 20 0;");
            vboxProducts.getChildren().add(runningHeader);

            FlowPane runningFlow = new FlowPane();
            runningFlow.setHgap(40);
            runningFlow.setVgap(40);

            for (Map<String, Object> auction : running) {
                runningFlow.getChildren().add(createAuctionItem(auction));
            }
            vboxProducts.getChildren().add(runningFlow);
        }

        if (!finished.isEmpty()) {
            Label finishedHeader = new Label("🔴 Đã kết thúc (" + finished.size() + ")");
            finishedHeader.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #aaaaaa; -fx-padding: 40 0 20 0;");
            vboxProducts.getChildren().add(finishedHeader);

            FlowPane finishedFlow = new FlowPane();
            finishedFlow.setHgap(40);
            finishedFlow.setVgap(40);

            for (Map<String, Object> auction : finished) {
                finishedFlow.getChildren().add(createAuctionItem(auction));
            }
            vboxProducts.getChildren().add(finishedFlow);
        }
    }

    private String categoryLabel(String category) {
        return MENU_TO_CATEGORY.entrySet().stream()
                .filter(e -> e.getValue().equalsIgnoreCase(category))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(category);
    }

    private String statusLabel(String status) {
        return MENU_TO_STATUS.entrySet().stream()
                .filter(e -> e.getValue().equals(status))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(status);
    }

    private String buildEmptyFilterMessage() {
        if (categoryFilter == null && statusFilter == null) {
            return "Hiện chưa có phiên đấu giá nào.";
        }
        StringBuilder msg = new StringBuilder("Không có phiên đấu giá");
        if (categoryFilter != null) {
            msg.append(" thuộc loại \"").append(categoryLabel(categoryFilter)).append("\"");
        }
        if (statusFilter != null) {
            if (categoryFilter != null) msg.append(" và");
            msg.append(" ở trạng thái \"").append(statusLabel(statusFilter)).append("\"");
        }
        msg.append(".");
        return msg.toString();
    }

    private void showMyAuctionResults() {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", UserSession.getInstance().getUserId());
        Response response = ServerConnection.getInstance().send("GET_MY_AUCTION_RESULTS", data);

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
                radioSocket = new java.net.Socket("10.11.5.168", 9999);
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
                            loadAuctionsFromServer();
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