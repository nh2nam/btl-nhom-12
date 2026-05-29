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
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyProductsController {

    // Tab labels
    @FXML private Label tabSelling;
    @FXML private Label tabBidding;
    @FXML private Label tabSoldDone;
    @FXML private Label tabBidDone;

    // Scroll panes (một per tab)
    @FXML private ScrollPane scrollSelling;
    @FXML private ScrollPane scrollBidding;
    @FXML private ScrollPane scrollSoldDone;
    @FXML private ScrollPane scrollBidDone;

    // FlowPanes
    @FXML private FlowPane paneSellingActive;
    @FXML private FlowPane paneBiddingActive;
    @FXML private FlowPane paneSoldDone;
    @FXML private FlowPane paneBidDone;

    // Count labels
    @FXML private Label lblSellingCount;
    @FXML private Label lblBiddingCount;
    @FXML private Label lblSoldCount;
    @FXML private Label lblBidDoneCount;
    @FXML private Label lblSummary;

    private Label activeTab;
    private ScrollPane activeScroll;

    private final Gson gson = new Gson();
    private final Map<String, Image> imageCache = new HashMap<>();

    // Style constants
    private static final String TAB_ACTIVE   = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 14 24 14 24; -fx-cursor: hand;";
    private static final String TAB_INACTIVE = "-fx-text-fill: #555; -fx-font-size: 14px; -fx-padding: 14 24 14 24; -fx-cursor: hand;";

    @FXML
    public void initialize() {
        activeTab    = tabSelling;
        activeScroll = scrollSelling;
        loadMyProducts();
    }

    // ─── Tab switching ────────────────────────────────────────────────────────

    @FXML
    private void switchTab(MouseEvent event) {
        Label clicked = (Label) event.getSource();
        if (clicked == activeTab) return;

        // Deactivate current
        activeTab.setStyle(TAB_INACTIVE);
        activeScroll.setVisible(false);

        // Activate new
        activeTab    = clicked;
        activeScroll = scrollForTab(clicked);
        activeTab.setStyle(TAB_ACTIVE + " -fx-text-fill: white; -fx-border-color: " + accentForTab(clicked) + "; -fx-border-width: 0 0 3 0;");
        activeScroll.setVisible(true);
    }

    private ScrollPane scrollForTab(Label tab) {
        if (tab == tabSelling)  return scrollSelling;
        if (tab == tabBidding)  return scrollBidding;
        if (tab == tabSoldDone) return scrollSoldDone;
        return scrollBidDone;
    }

    private String accentForTab(Label tab) {
        if (tab == tabSelling)  return "#CC0000";
        if (tab == tabBidding)  return "#4CAF50";
        if (tab == tabSoldDone) return "#888";
        return "#888";
    }

    // ─── Data loading ─────────────────────────────────────────────────────────

    private void loadMyProducts() {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", UserSession.getInstance().getUserId());
        Response response = ServerConnection.getInstance().send("GET_MY_PRODUCTS", data);

        if (!response.isSuccess()) {
            Platform.runLater(() -> lblSummary.setText("Không thể tải dữ liệu"));
            return;
        }

        Type mapType = new TypeToken<Map<String, List<Map<String, Object>>>>(){}.getType();
        Map<String, List<Map<String, Object>>> result;
        try {
            result = gson.fromJson(response.getData(), mapType);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        List<Map<String, Object>> selling  = result.getOrDefault("selling",  List.of());
        List<Map<String, Object>> bidding  = result.getOrDefault("bidding",  List.of());
        List<Map<String, Object>> soldDone = result.getOrDefault("soldDone", List.of());
        List<Map<String, Object>> bidDone  = result.getOrDefault("bidDone",  List.of());

        Platform.runLater(() -> {
            int total = selling.size() + bidding.size() + soldDone.size() + bidDone.size();
            lblSummary.setText(total + " sản phẩm  •  " + selling.size() + " đang bán  •  " + bidding.size() + " đang đấu giá");

            // Update tab labels with counts
            tabSelling.setText("🔴  Đang bán  " + badge(selling.size()));
            tabBidding.setText("🟢  Đang đấu giá  " + badge(bidding.size()));
            tabSoldDone.setText("📦  Đã bán  " + badge(soldDone.size()));
            tabBidDone.setText("🏁  Đã đấu giá  " + badge(bidDone.size()));

            lblSellingCount.setText(selling.size() + " sản phẩm đang được đấu giá");
            lblBiddingCount.setText(bidding.size() + " phiên bạn đang tham gia");
            lblSoldCount.setText(soldDone.size() + " sản phẩm đã hoàn tất");
            lblBidDoneCount.setText(bidDone.size() + " phiên đã kết thúc");

            renderCards(paneSellingActive, selling,  "selling");
            renderCards(paneBiddingActive, bidding,  "bidding");
            renderCards(paneSoldDone,      soldDone, "sold");
            renderCards(paneBidDone,       bidDone,  "bidDone");
        });
    }

    private String badge(int count) {
        return count > 0 ? "(" + count + ")" : "";
    }

    // ─── Card rendering ───────────────────────────────────────────────────────

    private void renderCards(FlowPane pane, List<Map<String, Object>> items, String type) {
        pane.getChildren().clear();
        if (items.isEmpty()) {
            pane.getChildren().add(buildEmptyState(type));
            return;
        }
        for (Map<String, Object> item : items) {
            pane.getChildren().add(createCard(item, type));
        }
    }

    private VBox buildEmptyState(String type) {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-padding: 60 0 60 0;");
        box.setPrefWidth(1100);

        String icon, msg;
        switch (type) {
            case "selling":  icon = "🏷️"; msg = "Bạn chưa đăng bán sản phẩm nào";       break;
            case "bidding":  icon = "🎯"; msg = "Bạn chưa tham gia phiên đấu giá nào";   break;
            case "sold":     icon = "📦"; msg = "Chưa có sản phẩm nào được bán thành công"; break;
            default:         icon = "🏁"; msg = "Chưa có phiên đấu giá nào kết thúc";    break;
        }

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 48px;");
        Label msgLbl = new Label(msg);
        msgLbl.setStyle("-fx-text-fill: #444; -fx-font-size: 16px;");

        box.getChildren().addAll(iconLbl, msgLbl);
        return box;
    }

    private VBox createCard(Map<String, Object> auction, String type) {
        boolean finished = type.equals("sold") || type.equals("bidDone");
        boolean isWinner = Boolean.TRUE.equals(auction.get("isWinner"));

        String itemName = (String) auction.getOrDefault("itemName", "Sản phẩm");
        double bid      = auction.get("currentHighestBid") instanceof Number
                ? ((Number) auction.get("currentHighestBid")).doubleValue() : 0;
        String endTime  = (String) auction.getOrDefault("endTime", "");

        // Màu accent theo loại tab
        String accent;
        switch (type) {
            case "selling": accent = "#CC0000"; break;
            case "bidding": accent = "#4CAF50"; break;
            default:        accent = "#333";    break;
        }

        // ── Root card ──
        VBox card = new VBox(0);
        card.setPrefWidth(280);
        card.setStyle(
            "-fx-background-color: #161616;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-color: #222;" +
            "-fx-border-width: 1;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 4);"
        );

        // ── Ảnh với overlay badge ──
        StackPane imgStack = new StackPane();
        imgStack.setPrefHeight(160);
        imgStack.setStyle("-fx-background-radius: 12 12 0 0; -fx-background-color: #1a1a1a;");

        ImageView imgView = new ImageView();
        imgView.setFitWidth(280);
        imgView.setFitHeight(160);
        imgView.setPreserveRatio(false);
        imgView.setStyle("-fx-background-radius: 12 12 0 0;");

        String imgPath = (String) auction.get("itemImagePath");
        if (imgPath != null && imgPath.startsWith("http")) {
            try {
                Image img = imageCache.computeIfAbsent(imgPath, k -> new Image(k, true));
                imgView.setImage(img);
            } catch (Exception ignored) {}
        }

        // Badge trạng thái góc trên trái
        Label badge = buildBadge(type, isWinner, finished);

        StackPane.setAlignment(badge, javafx.geometry.Pos.TOP_LEFT);
        imgStack.getChildren().addAll(imgView, badge);

        // ── Đường kẻ accent ──
        javafx.scene.layout.Region accentLine = new javafx.scene.layout.Region();
        accentLine.setPrefHeight(3);
        accentLine.setStyle("-fx-background-color: " + accent + ";");

        // ── Info box ──
        VBox info = new VBox(10);
        info.setStyle("-fx-padding: 16 16 16 16;");

        Label nameLabel = new Label(itemName);
        nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(248);

        // Giá
        HBox priceRow = new HBox(6);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        Label priceLbl = new Label("Giá hiện tại");
        priceLbl.setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");
        Label priceVal = new Label(String.format("%,.0f ₫", bid));
        priceVal.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 16px; -fx-font-weight: bold;");
        priceRow.getChildren().addAll(priceLbl, priceVal);

        // Thời gian
        HBox timeRow = new HBox(6);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        Label clockIcon = new Label(finished ? "🕐" : "⏳");
        clockIcon.setStyle("-fx-font-size: 12px;");
        Label timeLbl = new Label(endTime);
        timeLbl.setStyle("-fx-text-fill: #444; -fx-font-size: 12px;");
        timeRow.getChildren().addAll(clockIcon, timeLbl);

        info.getChildren().addAll(nameLabel, priceRow, timeRow);

        // Kết quả nếu đã kết thúc
        if (finished) {
            javafx.scene.layout.Region divider = new javafx.scene.layout.Region();
            divider.setPrefHeight(1);
            divider.setStyle("-fx-background-color: #222;");

            HBox resultRow = new HBox();
            resultRow.setAlignment(Pos.CENTER);
            resultRow.setStyle("-fx-padding: 10 0 0 0;");

            if (isWinner) {
                Label win = new Label("🏆  Đã thắng đấu giá");
                win.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 13px; -fx-font-weight: bold;");
                resultRow.getChildren().add(win);
            } else if (type.equals("sold")) {
                Label sold = new Label("✅  Đã bán thành công");
                sold.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 13px; -fx-font-weight: bold;");
                resultRow.getChildren().add(sold);
            } else {
                Label lose = new Label("❌  Không thắng phiên này");
                lose.setStyle("-fx-text-fill: #CC0000; -fx-font-size: 13px;");
                resultRow.getChildren().add(lose);
            }
            info.getChildren().addAll(divider, resultRow);
        }

        card.getChildren().addAll(imgStack, accentLine, info);

        // ── Hover effect ──
        String baseStyle =
            "-fx-background-color: #161616;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-color: #222;" +
            "-fx-border-width: 1;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 4);";
        String hoverStyle =
            "-fx-background-color: #1c1c1c;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-color: " + accent + ";" +
            "-fx-border-width: 1;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, " + accent + "55, 16, 0, 0, 0);";

        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e  -> card.setStyle(baseStyle));

        // ── Click ──
        card.setOnMouseClicked(e -> {
            Map<String, Object> full = new HashMap<>(auction);
            full.put("name", itemName);
            AppData.selectedAuction = full;
            Main.changeScene("/view/selling_product.fxml");
        });

        return card;
    }

    private Label buildBadge(String type, boolean isWinner, boolean finished) {
        String text, color;
        switch (type) {
            case "selling":
                text = "  ĐANG BÁN  "; color = "#CC0000"; break;
            case "bidding":
                text = "  ĐANG ĐẤU GIÁ  "; color = "#4CAF50"; break;
            case "sold":
                text = "  ĐÃ BÁN  "; color = "#2196F3"; break;
            default:
                text = isWinner ? "  🏆 THẮNG  " : "  ❌ THUA  ";
                color = isWinner ? "#FFD700" : "#CC0000";
                break;
        }
        Label badge = new Label(text);
        badge.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 10px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 0 0 6 0;" +
            "-fx-padding: 4 8 4 8;"
        );
        return badge;
    }

    @FXML
    private void goBack() {
        Main.changeScene("/view/home.fxml");
    }
}
