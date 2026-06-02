package com.auction.client.controller;

import com.auction.client.network.Response;
import com.auction.client.network.ServerConnection;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminController {

    // ── Tab buttons ──
    @FXML private Button btnUsers;
    @FXML private Button btnItems;

    // ── Panels ──
    @FXML private VBox panelUsers;
    @FXML private VBox panelItems;

    // ── User table ──
    @FXML private TableView<Map<String, Object>> tblUsers;
    @FXML private TableColumn<Map<String, Object>, String> colUserId;
    @FXML private TableColumn<Map<String, Object>, String> colUsername;
    @FXML private TableColumn<Map<String, Object>, String> colDisplayName;
    @FXML private TableColumn<Map<String, Object>, String> colEmail;
    @FXML private TableColumn<Map<String, Object>, String> colPhone;
    @FXML private TableColumn<Map<String, Object>, String> colRole;
    @FXML private TableColumn<Map<String, Object>, String> colUserAction;

    // ── Item table ──
    @FXML private TableView<Map<String, Object>> tblItems;
    @FXML private TableColumn<Map<String, Object>, String> colItemId;
    @FXML private TableColumn<Map<String, Object>, String> colItemName;
    @FXML private TableColumn<Map<String, Object>, String> colCategory;
    @FXML private TableColumn<Map<String, Object>, String> colStartPrice;
    @FXML private TableColumn<Map<String, Object>, String> colStatus;
    @FXML private TableColumn<Map<String, Object>, String> colCurrentBid;
    @FXML private TableColumn<Map<String, Object>, String> colEndTime;
    @FXML private TableColumn<Map<String, Object>, String> colItemAction;

    private final Gson gson = new Gson();

    // ── Shared styles ──
    private static final String CELL_STYLE =
            "-fx-text-fill: #dddddd; -fx-font-size: 13px; -fx-padding: 0 8 0 8; " +
            "-fx-background-color: transparent;";
    private static final String ROW_EVEN   = "-fx-background-color: #222222;";
    private static final String ROW_ODD    = "-fx-background-color: #1c1c1c;";
    private static final String ROW_HOVER  = "-fx-background-color: #2d2d45;";
    private static final String ROW_SELECT = "-fx-background-color: #3a3a6e;";

    @FXML
    public void initialize() {
        // Set resize policy bằng Java thay vì FXML (tránh lỗi coerce)
        tblUsers.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblItems.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        applyTableStyle(tblUsers);
        applyTableStyle(tblItems);
        setupUserTable();
        setupItemTable();
        showUsers();
    }

    // ─────────────────────────────────────────────
    // Tab switching
    // ─────────────────────────────────────────────

    @FXML
    private void showUsers() {
        panelUsers.setVisible(true);
        panelUsers.setManaged(true);
        panelItems.setVisible(false);
        panelItems.setManaged(false);
        btnUsers.setStyle(activeTabStyle());
        btnItems.setStyle(inactiveTabStyle());
        loadUsers();
    }

    @FXML
    private void showItems() {
        panelItems.setVisible(true);
        panelItems.setManaged(true);
        panelUsers.setVisible(false);
        panelUsers.setManaged(false);
        btnItems.setStyle(activeTabStyle());
        btnUsers.setStyle(inactiveTabStyle());
        loadItems();
    }

    // ─────────────────────────────────────────────
    // Table styling (replaces CSS)
    // ─────────────────────────────────────────────

    /** Áp style nền đen cho toàn bộ TableView bằng Java — không cần CSS file. */
    private <T> void applyTableStyle(TableView<T> table) {
        // Background của bảng
        table.setStyle(
            "-fx-background-color: #1e1e1e; " +
            "-fx-border-color: #2e2e2e; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6;"
        );

        // Row factory: màu xen kẽ + hover + selected
        table.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("-fx-background-color: transparent;");
                    } else {
                        updateRowStyle(this);
                    }
                }
            };
            // Cập nhật lại khi hover hoặc select thay đổi
            row.selectedProperty().addListener((obs, wasSelected, isSelected) ->
                    updateRowStyle(row));
            row.hoverProperty().addListener((obs, wasHover, isHover) ->
                    updateRowStyle(row));
            return row;
        });

        // Ẩn header mặc định background bằng cách set style trên header sau khi scene được tạo
        table.skinProperty().addListener((obs, oldSkin, newSkin) -> styleTableHeader(table));
    }

    private <T> void updateRowStyle(TableRow<T> row) {
        if (row.isEmpty()) {
            row.setStyle("-fx-background-color: transparent;");
            return;
        }
        if (row.isSelected()) {
            row.setStyle(ROW_SELECT);
        } else if (row.isHover()) {
            row.setStyle(ROW_HOVER);
        } else if (row.getIndex() % 2 == 0) {
            row.setStyle(ROW_EVEN);
        } else {
            row.setStyle(ROW_ODD);
        }
    }

    /** Style header của TableView sau khi skin đã được khởi tạo. */
    private void styleTableHeader(TableView<?> table) {
        // Tìm header region và tô màu
        javafx.scene.Node header = table.lookup("TableHeaderRow");
        if (header != null) {
            header.setStyle("-fx-background-color: #2a2a2a;");
        }
        table.lookupAll(".column-header").forEach(node ->
                node.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #3a3a3a; " +
                              "-fx-border-width: 0 1 1 0;"));
        table.lookupAll(".column-header .label").forEach(node ->
                node.setStyle("-fx-text-fill: #dddddd; -fx-font-weight: bold; -fx-font-size: 13px;"));
        table.lookupAll(".filler").forEach(node ->
                node.setStyle("-fx-background-color: #2a2a2a;"));
    }

    /** CellFactory chung: text trắng, nền trong suốt (lấy màu từ row). */
    private <T> TableCell<T, String> styledCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle(CELL_STYLE);
            }
        };
    }

    // ─────────────────────────────────────────────
    // User table setup & load
    // ─────────────────────────────────────────────

    private void setupUserTable() {
        colUserId.setCellValueFactory(p -> new SimpleStringProperty(
                String.valueOf(p.getValue().getOrDefault("id", ""))));
        colUsername.setCellValueFactory(p -> new SimpleStringProperty(
                String.valueOf(p.getValue().getOrDefault("username", ""))));
        colDisplayName.setCellValueFactory(p -> new SimpleStringProperty(
                String.valueOf(p.getValue().getOrDefault("displayName", ""))));
        colEmail.setCellValueFactory(p -> new SimpleStringProperty(
                String.valueOf(p.getValue().getOrDefault("email", ""))));
        colPhone.setCellValueFactory(p -> new SimpleStringProperty(
                String.valueOf(p.getValue().getOrDefault("phone", ""))));
        colRole.setCellValueFactory(p -> new SimpleStringProperty(
                String.valueOf(p.getValue().getOrDefault("role", ""))));

        // Áp styledCell cho tất cả cột text
        colUserId.setCellFactory(c -> styledCell());
        colUsername.setCellFactory(c -> styledCell());
        colDisplayName.setCellFactory(c -> styledCell());
        colEmail.setCellFactory(c -> styledCell());
        colPhone.setCellFactory(c -> styledCell());
        colRole.setCellFactory(c -> styledCell());

        // Cột action — nút Xóa
        colUserAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("Xóa");
            {
                btnDelete.setStyle(
                    "-fx-background-color: #cc0000; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4; " +
                    "-fx-padding: 4 12 4 12; -fx-font-size: 12px;"
                );
                btnDelete.setOnAction(e -> {
                    Map<String, Object> row = getTableView().getItems().get(getIndex());
                    confirmAndDeleteUser(row);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(CELL_STYLE);
                setGraphic(empty ? null : btnDelete);
            }
        });
    }

    private void loadUsers() {
        new Thread(() -> {
            Response resp = ServerConnection.getInstance().send("ADMIN_GET_USERS", new HashMap<>());
            Platform.runLater(() -> {
                if (resp.isSuccess() && resp.getData() != null) {
                    List<Map<String, Object>> list = gson.fromJson(
                            resp.getData(), new TypeToken<List<Map<String, Object>>>(){}.getType());
                    tblUsers.setItems(FXCollections.observableArrayList(list));
                    styleTableHeader(tblUsers);
                }
            });
        }).start();
    }

    private void confirmAndDeleteUser(Map<String, Object> row) {
        String username = String.valueOf(row.getOrDefault("username", ""));
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa người dùng: " + username);
        confirm.setContentText(
            "Thao tác này sẽ xóa toàn bộ:\n" +
            "• Phiên đấu giá\n• Giao dịch đặt giá\n• Tin nhắn chat\n\nKhông thể hoàn tác!");
        styleAlert(confirm);

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                int userId = ((Number) row.get("id")).intValue();
                new Thread(() -> {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("userId", userId);
                    Response resp = ServerConnection.getInstance().send("ADMIN_DELETE_USER", payload);
                    Platform.runLater(() -> {
                        showAlert(resp.isSuccess() ? Alert.AlertType.INFORMATION
                                                   : Alert.AlertType.ERROR,
                                resp.getMessage());
                        if (resp.isSuccess()) loadUsers();
                    });
                }).start();
            }
        });
    }

    // ─────────────────────────────────────────────
    // Item table setup & load
    // ─────────────────────────────────────────────

    private void setupItemTable() {
        colItemId.setCellValueFactory(p -> new SimpleStringProperty(
                String.valueOf(p.getValue().getOrDefault("id", ""))));
        colItemName.setCellValueFactory(p -> new SimpleStringProperty(
                String.valueOf(p.getValue().getOrDefault("name", ""))));
        colCategory.setCellValueFactory(p -> new SimpleStringProperty(
                String.valueOf(p.getValue().getOrDefault("category", ""))));
        colStartPrice.setCellValueFactory(p -> new SimpleStringProperty(
                formatPrice(p.getValue().getOrDefault("startingPrice", 0))));
        colStatus.setCellValueFactory(p -> new SimpleStringProperty(
                String.valueOf(p.getValue().getOrDefault("status", "N/A"))));
        colCurrentBid.setCellValueFactory(p -> new SimpleStringProperty(
                formatPrice(p.getValue().getOrDefault("currentBid", 0))));
        colEndTime.setCellValueFactory(p -> new SimpleStringProperty(
                String.valueOf(p.getValue().getOrDefault("endTime", "N/A"))));

        // Áp styledCell cho tất cả cột text
        colItemId.setCellFactory(c -> styledCell());
        colItemName.setCellFactory(c -> styledCell());
        colCategory.setCellFactory(c -> styledCell());
        colStartPrice.setCellFactory(c -> styledCell());
        colStatus.setCellFactory(c -> styledCell());
        colCurrentBid.setCellFactory(c -> styledCell());
        colEndTime.setCellFactory(c -> styledCell());

        // Cột action — nút Xóa
        colItemAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("Xóa");
            {
                btnDelete.setStyle(
                    "-fx-background-color: #cc0000; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4; " +
                    "-fx-padding: 4 12 4 12; -fx-font-size: 12px;"
                );
                btnDelete.setOnAction(e -> {
                    Map<String, Object> row = getTableView().getItems().get(getIndex());
                    confirmAndDeleteItem(row);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(CELL_STYLE);
                setGraphic(empty ? null : btnDelete);
            }
        });
    }

    private void loadItems() {
        new Thread(() -> {
            Response resp = ServerConnection.getInstance().send("ADMIN_GET_ITEMS", new HashMap<>());
            Platform.runLater(() -> {
                if (resp.isSuccess() && resp.getData() != null) {
                    List<Map<String, Object>> list = gson.fromJson(
                            resp.getData(), new TypeToken<List<Map<String, Object>>>(){}.getType());
                    tblItems.setItems(FXCollections.observableArrayList(list));
                    styleTableHeader(tblItems);
                }
            });
        }).start();
    }

    private void confirmAndDeleteItem(Map<String, Object> row) {
        String name = String.valueOf(row.getOrDefault("name", ""));
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa sản phẩm: " + name);
        confirm.setContentText(
            "Thao tác này sẽ xóa toàn bộ:\n" +
            "• Phiên đấu giá liên quan\n• Giao dịch đặt giá\n• Tin nhắn chat\n\nKhông thể hoàn tác!");
        styleAlert(confirm);

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                int itemId = ((Number) row.get("id")).intValue();
                new Thread(() -> {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("itemId", itemId);
                    Response resp = ServerConnection.getInstance().send("ADMIN_DELETE_ITEM", payload);
                    Platform.runLater(() -> {
                        showAlert(resp.isSuccess() ? Alert.AlertType.INFORMATION
                                                   : Alert.AlertType.ERROR,
                                resp.getMessage());
                        if (resp.isSuccess()) loadItems();
                    });
                }).start();
            }
        });
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private String formatPrice(Object val) {
        if (val == null) return "0$";
        try { return String.format("%.0f$", ((Number) val).doubleValue()); }
        catch (Exception e) { return val + "$"; }
    }

    private String activeTabStyle() {
        return "-fx-background-color: #ffffff; -fx-text-fill: #111111; " +
               "-fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand; " +
               "-fx-background-radius: 6 6 0 0; -fx-padding: 10 28 10 28;";
    }

    private String inactiveTabStyle() {
        return "-fx-background-color: #2e2e2e; -fx-text-fill: #888888; " +
               "-fx-font-size: 14px; -fx-cursor: hand; " +
               "-fx-background-radius: 6 6 0 0; -fx-padding: 10 28 10 28;";
    }

    private void styleAlert(Alert alert) {
        alert.getDialogPane().setStyle(
            "-fx-background-color: #1e1e1e;");

        // Header pane (chứa tiêu đề + icon)
        javafx.scene.Node headerPanel = alert.getDialogPane().lookup(".header-panel");
        if (headerPanel != null) {
            headerPanel.setStyle("-fx-background-color: #1e1e1e;");
        }

        // Header text
        javafx.scene.Node headerText = alert.getDialogPane().lookup(".header-panel .label");
        if (headerText != null) {
            headerText.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 15px; -fx-font-weight: bold;");
        }

        // Ẩn icon hỏi chấm
        javafx.scene.Node graphic = alert.getDialogPane().lookup(".graphic-container");
        if (graphic != null) {
            graphic.setStyle("-fx-padding: 0;");
            ((javafx.scene.layout.Region) graphic).setMaxWidth(0);
            ((javafx.scene.layout.Region) graphic).setMaxHeight(0);
        }

        // Content text
        javafx.scene.Node content = alert.getDialogPane().lookup(".content.label");
        if (content != null) {
            content.setStyle("-fx-text-fill: #dddddd; -fx-font-size: 13px;");
        }

        // Button bar
        javafx.scene.Node buttonBar = alert.getDialogPane().lookup(".button-bar");
        if (buttonBar != null) {
            buttonBar.setStyle("-fx-background-color: #1e1e1e;");
        }

        // Buttons
        alert.getDialogPane().getButtonTypes().forEach(bt -> {
            javafx.scene.Node btn = alert.getDialogPane().lookupButton(bt);
            if (bt == ButtonType.OK || bt == ButtonType.YES) {
                btn.setStyle("-fx-background-color: #cc0000; -fx-text-fill: white; " +
                             "-fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4;");
            } else {
                btn.setStyle("-fx-background-color: #444444; -fx-text-fill: #eeeeee; " +
                             "-fx-cursor: hand; -fx-background-radius: 4;");
            }
        });
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() {
        com.auction.client.session.UserSession.getInstance().logout();
        com.auction.client.Main.changeScene("/view/login.fxml");
    }
}
