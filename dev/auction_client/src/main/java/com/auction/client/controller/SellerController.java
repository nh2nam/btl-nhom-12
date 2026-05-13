package com.auction.client.controller;

import com.auction.client.model.AppData;
import com.auction.client.session.UserSession;
import com.auction.client.network.Response;
import com.auction.client.network.ServerConnection;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static com.auction.client.model.AppData.currentUserId;

public class SellerController {

    @FXML private Label lblUsername;
    @FXML private VBox vboxProducts;
    @FXML private Button btnBuy;
    @FXML private Button btnLogout;
    @FXML private Button btnAddProduct;

    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        lblUsername.setText(AppData.username);
        
        loadMyProductsFromServer();

        btnBuy.setOnAction(event -> goToBuyScreen());
        btnLogout.setOnAction(event -> logout());
        btnAddProduct.setOnAction(event -> addNewProduct());
    }

    private void loadMyProductsFromServer() {
        Response response = ServerConnection.getInstance().send("GET_AUCTIONS", null);

        if (!response.isSuccess()) {
            showError("Không thể tải sản phẩm: " + response.getMessage());
            return;
        }

        Object rawData = response.getData();
        List<Map<String, Object>> auctions = null;
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();

        try {
            if (rawData instanceof String) {
                auctions = gson.fromJson((String) rawData, listType);
            } else if (rawData instanceof List) {
                auctions = (List<Map<String, Object>>) rawData;
            } else {
                String jsonStr = gson.toJson(rawData);
                auctions = gson.fromJson(jsonStr, listType);
            }
        } catch (Exception e) {
            showError("Lỗi đọc dữ liệu từ Server!");
            return;
        }

        vboxProducts.getChildren().clear();
        if (auctions == null || auctions.isEmpty()) {
            showError("Bạn chưa đăng bán sản phẩm nào.");
            return;
        }

        boolean hasProducts = false;

        for (Map<String, Object> auction : auctions) {
            // Lọc: Chỉ lấy những phiên đấu giá do tài khoản này tạo
            int sellerId = auction.get("sellerId") != null ? ((Number) auction.get("sellerId")).intValue() : -1;

            if (sellerId == currentUserId) {
                vboxProducts.getChildren().add(createProductItem(auction));
                hasProducts = true;
            }
        }

        if (!hasProducts) {
            showError("Bạn chưa đăng bán sản phẩm nào.");
        }
    }

    private HBox createProductItem(Map<String, Object> auction) {
        HBox itembox = new HBox(15);
        itembox.setStyle("-fx-padding: 15; -fx-background-color: white; -fx-border-color: #eee; -fx-border-width: 0 0 1 0; -fx-alignment: CENTER_LEFT;");

        String itemName = (String) auction.getOrDefault("name", "Sản phẩm");
        double price = auction.get("startingPrice") != null ? ((Number) auction.get("startingPrice")).doubleValue() : 0;
        String imgPath = (String) auction.get("itemImagePath");
        String status = (String) auction.getOrDefault("status", "UNKNOWN");

        ImageView imgView = new ImageView();
        try {
            if (imgPath != null && !imgPath.trim().isEmpty()) {
                if (imgPath.startsWith("http")) {
                    // Nếu là ảnh lấy từ web
                    imgView.setImage(new Image(imgPath));
                } else if (imgPath.length() > 500) {
                    // Nếu là chuỗi siêu dài -> Chắc chắn là ảnh Base64
                    byte[] imageBytes = Base64.getDecoder().decode(imgPath);
                    imgView.setImage(new Image(new ByteArrayInputStream(imageBytes)));
                } else {
                    // Đọc từ local resource (cách cũ phòng hờ)
                    java.io.InputStream imageStream = getClass().getResourceAsStream(imgPath);
                    if (imageStream != null) {
                        imgView.setImage(new Image(imageStream));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi giải mã và nạp ảnh: " + e.getMessage());
        }

        imgView.setFitWidth(100);
        imgView.setFitHeight(100);
        imgView.setPreserveRatio(true);

        VBox info = new VBox(8);
        Label name = new Label(itemName);
        name.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label priceLabel = new Label(String.format("Giá hiện tại: %,.0f VNĐ", price));
        priceLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 16px;");

        Label statusLabel = new Label("Trạng thái: " + status);
        statusLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 14px;");

        info.getChildren().addAll(name, priceLabel, statusLabel);
        itembox.getChildren().addAll(imgView, info);

        itembox.setOnMouseEntered(e -> itembox.setStyle("-fx-background-color: #fafafa; -fx-padding: 15; -fx-border-color: #eee; -fx-border-width: 0 0 1 0; -fx-alignment: CENTER_LEFT;"));
        itembox.setOnMouseExited(e -> itembox.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-border-color: #eee; -fx-border-width: 0 0 1 0; -fx-alignment: CENTER_LEFT;"));

        return itembox;
    }

    private void showError(String msg) {
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill: #888; -fx-font-size: 14px; -fx-padding: 20;");
        vboxProducts.getChildren().clear();
        vboxProducts.getChildren().add(lbl);
    }

    @FXML
    private void goToBuyScreen() {
        com.auction.client.Main.changeScene("/view/home.fxml");
    }

    @FXML
    private void logout() {
        UserSession.getInstance().logout();
        com.auction.client.Main.changeScene("/view/login.fxml");
    }

    @FXML
    private void addNewProduct() {
        com.auction.client.Main.changeScene("/view/add_product.fxml");
    }
}