package com.auction.client.controller;

import com.auction.client.Main;
import com.auction.client.model.AppData;
import com.auction.client.network.Response;
import com.auction.client.network.ServerConnection;
import com.auction.client.session.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.nio.file.Files;
import java.util.Base64;
import java.io.File;
import java.util.HashMap;
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

    private File selectedImageFile;

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
    void handleChooseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) lblImagePath.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            selectedImageFile = file;
            lblImagePath.setText(file.getName());
            lblMessage.setText("");
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

        // Đóng gói dữ liệu gửi lên Server
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("description", desc);
        data.put("startingPrice", price);
        String category = cbCategory.getValue(); // Lấy giá trị người dùng đã chọn
        data.put("category", category); // Đút vào gói dữ liệu gửi đi

        String imgPath = "";
        if (selectedImageFile != null) {
            try {
                // Đọc file ảnh thành mảng byte
                byte[] fileContent = Files.readAllBytes(selectedImageFile.toPath());
                // Mã hóa thành chuỗi Base64
                imgPath = Base64.getEncoder().encodeToString(fileContent);
            } catch (Exception e) {
                System.err.println("Lỗi mã hóa ảnh: " + e.getMessage());
            }
        }
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
        selectedImageFile = null;
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
    void goToHome(ActionEvent event) {
        Main.changeScene("/view/home.fxml");
    }


    @FXML
    void logout(ActionEvent event) {
        UserSession.getInstance().logout();
        Main.changeScene("/view/login.fxml");
    }
}