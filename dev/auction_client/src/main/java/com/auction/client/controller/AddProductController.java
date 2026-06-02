package com.auction.client.controller;

import com.auction.client.Main;
import com.auction.client.model.AppData;
import com.auction.client.network.Response;
import com.auction.client.network.ServerConnection;
import com.auction.client.session.UserSession;
import com.auction.client.network.CloudinaryUtil;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AddProductController {
    @FXML private ComboBox<String> cbCategory;
    @FXML private TextField  txtName;
    @FXML private TextField  txtPrice;
    @FXML private TextArea   txtDesc;
    @FXML private Label      lblImagePath;
    @FXML private Label      lblMessage;
    @FXML private Label      lblUsername;
    @FXML private TextField  txtDuration;
    @FXML private ComboBox<String> cbDurationUnit;
    @FXML private HBox       hboxImagePreview;

    private static final int MAX_IMAGES = 5;
    private final List<File> selectedImageFiles = new ArrayList<>();

    @FXML
    public void initialize() {
        lblUsername.setText(AppData.username);

        // Các đơn vị thời gian
        cbDurationUnit.getItems().addAll("Phút", "Giờ", "Ngày");
        cbDurationUnit.setValue("Giờ"); // Mặc định là Giờ
        cbCategory.getItems().addAll("Electronics", "Arts", "Fashion", "RealEstate", "Other");
        cbCategory.setValue("Other"); // Để mặc định là Other
    }

    @FXML
    void handleChooseImages(ActionEvent event) {
        if (selectedImageFiles.size() >= MAX_IMAGES) {
            showErrorMessage("Bạn đã chọn tối đa " + MAX_IMAGES + " ảnh!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) lblImagePath.getScene().getWindow();
        List<File> files = fileChooser.showOpenMultipleDialog(stage);

        if (files != null && !files.isEmpty()) {
            int canAdd = MAX_IMAGES - selectedImageFiles.size();
            List<File> toAdd = files.subList(0, Math.min(files.size(), canAdd));
            selectedImageFiles.addAll(toAdd);
            if (files.size() > canAdd) {
                showErrorMessage("Chỉ thêm được " + canAdd + " ảnh nữa. Đã đạt giới hạn " + MAX_IMAGES + " ảnh.");
            } else {
                lblMessage.setText("");
            }
            refreshImagePreview();
        }
    }

    @FXML
    void handleClearImages(ActionEvent event) {
        selectedImageFiles.clear();
        refreshImagePreview();
    }

    /** Cập nhật label đếm và dải thumbnail nhỏ bên dưới nút chọn ảnh */
    private void refreshImagePreview() {
        int count = selectedImageFiles.size();
        lblImagePath.setText(count == 0 ? "[Chưa có ảnh]" : count + " ảnh đã chọn");

        hboxImagePreview.getChildren().clear();
        for (int i = 0; i < selectedImageFiles.size(); i++) {
            final int idx = i;
            File f = selectedImageFiles.get(i);

            ImageView thumb = new ImageView(new Image(f.toURI().toString(), 80, 80, true, true));
            thumb.setFitWidth(80);
            thumb.setFitHeight(80);
            thumb.setPreserveRatio(true);

            // Nút X để xóa từng ảnh
            Text xBtn = new Text("✕");
            xBtn.setFill(Color.WHITE);
            xBtn.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            StackPane badge = new StackPane(xBtn);
            badge.setPrefSize(20, 20);
            badge.setMaxSize(20, 20);
            badge.setStyle("-fx-background-color: rgba(200,0,0,0.85); -fx-background-radius: 10; -fx-cursor: hand;");
            badge.setOnMouseClicked(e -> {
                selectedImageFiles.remove(idx);
                refreshImagePreview();
            });

            StackPane cell = new StackPane();
            cell.setPrefSize(90, 90);
            cell.setMaxSize(90, 90);
            cell.setStyle("-fx-background-color: #2d2d2d; -fx-background-radius: 6; -fx-border-color: #555; -fx-border-radius: 6;");

            Rectangle clip = new Rectangle(80, 80);
            clip.setArcWidth(6);
            clip.setArcHeight(6);
            thumb.setClip(clip);

            cell.getChildren().add(thumb);
            StackPane.setAlignment(badge, javafx.geometry.Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(2, 2, 0, 0));
            cell.getChildren().add(badge);

            hboxImagePreview.getChildren().add(cell);
        }
    }

    @FXML
    void saveProduct(ActionEvent event) {
        String name     = txtName.getText().trim();
        String priceStr = txtPrice.getText().trim();
        String desc     = txtDesc.getText().trim();

        if (name.isEmpty() || priceStr.isEmpty() || desc.isEmpty()) {
            showErrorMessage("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0) {
                showErrorMessage("Giá sản phẩm phải lớn hơn 0!");
                return;
            }
        } catch (NumberFormatException e) {
            showErrorMessage("Giá tiền phải là một con số hợp lệ!");
            return;
        }

        // Validate thời gian
        String durationStr = txtDuration.getText().trim();
        if (durationStr.isEmpty()) {
            showErrorMessage("Vui lòng nhập thời gian đấu giá!");
            return;
        }
        long durationMinutes;
        try {
            long durationValue = Long.parseLong(durationStr);
            if (durationValue <= 0) {
                showErrorMessage("Thời gian phải lớn hơn 0!");
                return;
            }
            String unit = cbDurationUnit.getValue();
            switch (unit) {
                case "Phút": durationMinutes = durationValue;        break;
                case "Giờ":  durationMinutes = durationValue * 60;   break;
                default:     durationMinutes = durationValue * 1440; break; // Ngày
            }
        } catch (NumberFormatException e) {
            showErrorMessage("Thời gian phải là một số nguyên hợp lệ!");
            return;
        }

        // --- ĐOẠN CODE XỬ LÝ NHIỀU ẢNH (CLOUDINARY) ---
        List<String> uploadedUrls = new ArrayList<>();
        if (!selectedImageFiles.isEmpty()) {
            showErrorMessage("Đang tải " + selectedImageFiles.size() + " ảnh lên Cloud, vui lòng đợi...");

            for (File imgFile : selectedImageFiles) {
                String uploadedUrl = CloudinaryUtil.uploadImage(imgFile);
                if (uploadedUrl != null && !uploadedUrl.isEmpty()) {
                    uploadedUrls.add(uploadedUrl);
                } else {
                    showErrorMessage("Lỗi tải ảnh '" + imgFile.getName() + "' lên Cloud! Vui lòng thử lại.");
                    return;
                }
            }
        }

        // Nối các URL bằng dấu phẩy để lưu vào 1 trường imagePath
        String imgPath = String.join(",", uploadedUrls);

        // Đóng gói dữ liệu gửi lên Server
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("description", desc);
        data.put("startingPrice", price);
        String category = cbCategory.getValue();
        data.put("category", category);

        // Gửi Link URL vừa lấy được từ Cloudinary (Thay vì Base64)
        data.put("imagePath", imgPath);

        // Gửi kèm thời gian đấu giá (đơn vị: phút)
        data.put("durationMinutes", durationMinutes);

        // QUAN TRỌNG: Gửi kèm ID của người đang đăng nhập
        data.put("sellerId", UserSession.getInstance().getUserId());

        Response response = ServerConnection.getInstance().send("ADD_ITEM", data);

        if (response.isSuccess()) {
            lblMessage.setStyle("-fx-text-fill: green;");
            lblMessage.setText(response.getMessage());
            clearFields();
        } else {
            showErrorMessage(response.getMessage());
        }
    }

    private void showErrorMessage(String message) {
        lblMessage.setStyle("-fx-text-fill: red;");
        lblMessage.setText(message);
    }

    private void clearFields() {
        txtName.clear();
        txtPrice.clear();
        txtDesc.clear();
        lblImagePath.setText("[Chưa có ảnh]");
        selectedImageFiles.clear();
        hboxImagePreview.getChildren().clear();
        txtDuration.clear();
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
    void goToHome() {
        Main.changeScene("/view/home.fxml");
    }

    @FXML
    void logout() {
        UserSession.getInstance().logout();
        Main.changeScene("/view/login.fxml");
    }
}