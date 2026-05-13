package com.auction.client.controller;

import com.auction.client.Main;
import com.auction.client.network.Response;
import com.auction.client.network.ServerConnection;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

public class SignUpController {

    @FXML private TextField     txtUser, txtAddress, txtPhone, txtAccountName;
    @FXML private PasswordField txtPass;
    @FXML private Label         lblMessage;
    @FXML private Button        btnAction;

    private boolean isSuccess = false;

    @FXML
    private void handleAction() {
        if (isSuccess) {
            Main.changeScene("/view/login.fxml");
            return;
        }

        String username    = txtUser.getText().trim();
        String accountName = txtAccountName.getText().trim();
        String password    = txtPass.getText().trim();
        String address     = txtAddress.getText().trim();
        String phone       = txtPhone.getText().trim();

        if (username.isEmpty() || address.isEmpty() || phone.isEmpty()
                || accountName.isEmpty() || password.isEmpty()) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Nhập thiếu thông tin!");
            return;
        }

        if (accountName.length() < 3) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Tên đăng nhập phải có ít nhất 3 ký tự!");
            return;
        }

        if (password.length() < 6) {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText("Mật khẩu phải có ít nhất 6 ký tự!");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("username", accountName);
        data.put("account_name",    username );
        data.put("password", password);
        data.put("role",     "BIDDER"); // Tất cả tài khoản đều có quyền mua và bán

        Response response = ServerConnection.getInstance().send("REGISTER", data);

        if (response.isSuccess()) {
            lblMessage.setTextFill(Color.GREEN);
            lblMessage.setText("Tạo tài khoản thành công!");
            btnAction.setText("Quay lại đăng nhập");
            isSuccess = true;
        } else {
            lblMessage.setTextFill(Color.RED);
            lblMessage.setText(response.getMessage());
        }
    }

    @FXML
    private void goToLogin() {
        Main.changeScene("/view/login.fxml");
    }
}

