package com.auction.client.controller;

import com.auction.client.Main;
import com.auction.client.network.Response;
import com.auction.client.network.ServerConnection;
import com.auction.client.session.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.HashMap;
import java.util.Map;

import static com.auction.client.model.AppData.currentUserId;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            lblError.setText("Chưa điền đầy đủ thông tin");
            return;
        }

        // Gửi request LOGIN lên server
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("password", password);

        Response response = ServerConnection.getInstance().send("LOGIN", data);
        if (response.isSuccess()) {
            System.out.println("check");
            // Lưu thông tin user vào session
            @SuppressWarnings("unchecked")
            // Dùng Gson để dịch chuỗi JSON (String) thành đối tượng Map
            com.google.gson.Gson gson = new com.google.gson.Gson();
            Map<String, Object> userInfo = gson.fromJson(response.getData(), java.util.Map.class);
            if (userInfo != null) {
                UserSession.getInstance().login(
                        ((Number) userInfo.get("id")).intValue(),
                        (String) userInfo.get("username"),
                        (String) userInfo.getOrDefault("displayName", userInfo.get("username")),
                        (String) userInfo.get("role")
                );
            }
            currentUserId = UserSession.getInstance().getUserId();
            Main.changeScene("/view/home.fxml");
        } else {
            lblError.setText(response.getMessage());
        }
    }

    @FXML
    private void goToSignUp() {
        Main.changeScene("/view/signup.fxml");
    }
}
