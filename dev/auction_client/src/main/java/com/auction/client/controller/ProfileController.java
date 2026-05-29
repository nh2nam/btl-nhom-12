package com.auction.client.controller;

import com.auction.client.Main;
import com.auction.client.network.Response;
import com.auction.client.network.ServerConnection;
import com.auction.client.session.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

public class ProfileController {

    @FXML private Label lblDisplayName;
    @FXML private Label lblRole;

    // --- Email ---
    @FXML private Label     lblEmail;
    @FXML private TextField txtEmail;
    @FXML private Button    btnEditEmail;
    @FXML private javafx.scene.layout.HBox hboxEmailButtons;
    @FXML private Label     lblEmailMsg;

    // --- Số điện thoại ---
    @FXML private Label     lblPhone;
    @FXML private TextField txtPhone;
    @FXML private Button    btnEditPhone;
    @FXML private javafx.scene.layout.HBox hboxPhoneButtons;
    @FXML private Label     lblPhoneMsg;

    @FXML
    public void initialize() {
        UserSession s = UserSession.getInstance();
        lblDisplayName.setText(s.getDisplayName());

        String roleText;
        switch (s.getRole() != null ? s.getRole() : "") {
            case "SELLER": roleText = "Người bán";     break;
            case "ADMIN":  roleText = "Quản trị viên"; break;
            default:       roleText = "Người đấu giá"; break;
        }
        lblRole.setText(roleText);

        refreshEmail();
        refreshPhone();
        setEmailViewMode();
        setPhoneViewMode();
    }

    // ===================== EMAIL =====================

    private void refreshEmail() {
        String email = UserSession.getInstance().getEmail();
        lblEmail.setText(email == null || email.isEmpty() ? "Chưa cập nhật" : email);
    }

    private void setEmailViewMode() {
        lblEmail.setVisible(true);   lblEmail.setManaged(true);
        txtEmail.setVisible(false);  txtEmail.setManaged(false);
        hboxEmailButtons.setVisible(false); hboxEmailButtons.setManaged(false);
        btnEditEmail.setVisible(true); btnEditEmail.setManaged(true);
        lblEmailMsg.setText("");
    }

    @FXML
    private void handleEditEmail() {
        String cur = UserSession.getInstance().getEmail();
        txtEmail.setText(cur == null || cur.equals("Chưa cập nhật") ? "" : cur);
        lblEmail.setVisible(false);  lblEmail.setManaged(false);
        txtEmail.setVisible(true);   txtEmail.setManaged(true);
        hboxEmailButtons.setVisible(true); hboxEmailButtons.setManaged(true);
        btnEditEmail.setVisible(false); btnEditEmail.setManaged(false);
        lblEmailMsg.setText("");
        txtEmail.requestFocus();
    }

    @FXML
    private void handleCancelEmail() { setEmailViewMode(); }

    @FXML
    private void handleSaveEmail() {
        String newEmail = txtEmail.getText().trim();
        if (newEmail.isEmpty()) {
            lblEmailMsg.setTextFill(Color.RED);
            lblEmailMsg.setText("Email không được để trống!");
            return;
        }
        if (!newEmail.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
            lblEmailMsg.setTextFill(Color.RED);
            lblEmailMsg.setText("Email không hợp lệ!");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", UserSession.getInstance().getUserId());
        data.put("email",  newEmail);

        Response response = ServerConnection.getInstance().send("UPDATE_EMAIL", data);
        if (response.isSuccess()) {
            UserSession.getInstance().setEmail(newEmail);
            refreshEmail();
            setEmailViewMode();
            lblEmailMsg.setTextFill(Color.GREEN);
            lblEmailMsg.setText("✓ Cập nhật thành công!");
        } else {
            lblEmailMsg.setTextFill(Color.RED);
            lblEmailMsg.setText(response.getMessage());
        }
    }

    // ===================== PHONE =====================

    private void refreshPhone() {
        String phone = UserSession.getInstance().getPhone();
        lblPhone.setText(phone == null || phone.isEmpty() ? "Chưa cập nhật" : phone);
    }

    private void setPhoneViewMode() {
        lblPhone.setVisible(true);   lblPhone.setManaged(true);
        txtPhone.setVisible(false);  txtPhone.setManaged(false);
        hboxPhoneButtons.setVisible(false); hboxPhoneButtons.setManaged(false);
        btnEditPhone.setVisible(true); btnEditPhone.setManaged(true);
        lblPhoneMsg.setText("");
    }

    @FXML
    private void handleEditPhone() {
        String cur = UserSession.getInstance().getPhone();
        txtPhone.setText(cur == null || cur.equals("Chưa cập nhật") ? "" : cur);
        lblPhone.setVisible(false);  lblPhone.setManaged(false);
        txtPhone.setVisible(true);   txtPhone.setManaged(true);
        hboxPhoneButtons.setVisible(true); hboxPhoneButtons.setManaged(true);
        btnEditPhone.setVisible(false); btnEditPhone.setManaged(false);
        lblPhoneMsg.setText("");
        txtPhone.requestFocus();
    }

    @FXML
    private void handleCancelPhone() { setPhoneViewMode(); }

    @FXML
    private void handleSavePhone() {
        String newPhone = txtPhone.getText().trim();
        if (newPhone.isEmpty()) {
            lblPhoneMsg.setTextFill(Color.RED);
            lblPhoneMsg.setText("Số điện thoại không được để trống!");
            return;
        }
        if (!newPhone.matches("\\d{9,11}")) {
            lblPhoneMsg.setTextFill(Color.RED);
            lblPhoneMsg.setText("Số điện thoại không hợp lệ (9-11 chữ số)!");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", UserSession.getInstance().getUserId());
        data.put("phone",  newPhone);

        Response response = ServerConnection.getInstance().send("UPDATE_PHONE", data);
        if (response.isSuccess()) {
            UserSession.getInstance().setPhone(newPhone);
            refreshPhone();
            setPhoneViewMode();
            lblPhoneMsg.setTextFill(Color.GREEN);
            lblPhoneMsg.setText("✓ Cập nhật thành công!");
        } else {
            lblPhoneMsg.setTextFill(Color.RED);
            lblPhoneMsg.setText(response.getMessage());
        }
    }

    // ===================== NAVIGATION =====================

    @FXML
    private void goBack() {
        Main.changeScene("/view/home.fxml");
    }
}
