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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MyProductsController {

    @FXML private StackPane rootPane;

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
        String status    = (String) auction.getOrDefault("status", "");

        String itemName = (String) auction.getOrDefault("itemName", "Sản phẩm");
        double bid      = auction.get("currentHighestBid") instanceof Number
                ? ((Number) auction.get("currentHighestBid")).doubleValue() : 0;
        String endTime  = (String) auction.getOrDefault("endTime", "");

        // Badge màu PENDING_PAYMENT cho winner trong tab bidDone
        boolean isPendingPayment = "PENDING_PAYMENT".equals(status) && isWinner;
        boolean isCancelled      = "CANCELLED".equals(status);

        // Màu accent theo loại tab
        String accent;
        if (isPendingPayment) {
            accent = "#FF9800";
        } else {
            switch (type) {
                case "selling": accent = "#CC0000"; break;
                case "bidding": accent = "#4CAF50"; break;
                default:        accent = "#333";    break;
            }
        }

        // ── Root card ──
        VBox card = new VBox(0);
        card.setPrefWidth(280);
        card.setStyle(
            "-fx-background-color: #161616;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-color: " + (isPendingPayment ? "#FF9800" : "#222") + ";" +
            "-fx-border-width: " + (isPendingPayment ? "2" : "1") + ";" +
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

        String rawImgPath = (String) auction.get("itemImagePath");
        String imgPath = (rawImgPath != null && !rawImgPath.trim().isEmpty())
                ? rawImgPath.split(",")[0].trim()
                : null;
        if (imgPath != null && imgPath.startsWith("http")) {
            try {
                final String cacheKey = imgPath;
                Image img = imageCache.computeIfAbsent(cacheKey, k -> new Image(k, true));
                imgView.setImage(img);
            } catch (Exception ignored) {}
        }

        // Badge trạng thái góc trên trái
        Label badge = buildBadge(type, isWinner, finished, status);
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
        Label clockIcon = new Label(finished || isPendingPayment ? "🕐" : "⏳");
        clockIcon.setStyle("-fx-font-size: 12px;");
        Label timeLbl = new Label(endTime);
        timeLbl.setStyle("-fx-text-fill: #444; -fx-font-size: 12px;");
        timeRow.getChildren().addAll(clockIcon, timeLbl);

        info.getChildren().addAll(nameLabel, priceRow, timeRow);

        // Nếu đang chờ thanh toán (winner) → hiện nhãn nhắc nhở
        if (isPendingPayment) {
            javafx.scene.layout.Region divider = new javafx.scene.layout.Region();
            divider.setPrefHeight(1);
            divider.setStyle("-fx-background-color: #2a2a2a;");

            Label pendingLbl = new Label("⏳  Đang chờ thanh toán\nNhấp vào để thanh toán / hủy");
            pendingLbl.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 12px; -fx-font-weight: bold;");
            pendingLbl.setWrapText(true);
            info.getChildren().addAll(divider, pendingLbl);
        }
        // Kết quả nếu đã kết thúc bình thường
        else if (finished) {
            javafx.scene.layout.Region divider = new javafx.scene.layout.Region();
            divider.setPrefHeight(1);
            divider.setStyle("-fx-background-color: #222;");

            HBox resultRow = new HBox();
            resultRow.setAlignment(Pos.CENTER);
            resultRow.setStyle("-fx-padding: 10 0 0 0;");

            if ("CANCELLED".equals(status) && isWinner) {
                Label cancelled = new Label("⛔  Đã hủy thanh toán");
                cancelled.setStyle("-fx-text-fill: #888; -fx-font-size: 13px;");
                resultRow.getChildren().add(cancelled);
            } else if (isWinner) {
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
            "-fx-border-color: " + (isPendingPayment ? "#FF9800" : "#222") + ";" +
            "-fx-border-width: " + (isPendingPayment ? "2" : "1") + ";" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 4);";
        String hoverStyle =
            "-fx-background-color: #1c1c1c;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-color: " + accent + ";" +
            "-fx-border-width: " + (isPendingPayment ? "2" : "1") + ";" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, " + accent + "88, 18, 0, 0, 0);";

        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e  -> card.setStyle(baseStyle));

        // ── Click ──
        card.setOnMouseClicked(e -> {
            if (isPendingPayment) {
                // Mở dialog thanh toán với đếm ngược
                openPaymentDialog(auction);
            } else {
                Map<String, Object> full = new HashMap<>(auction);
                full.put("name", itemName);
                AppData.selectedAuction = full;
                Main.changeScene("/view/selling_product.fxml");
            }
        });

        return card;
    }

    private Label buildBadge(String type, boolean isWinner, boolean finished, String status) {
        String text, color;

        // Ưu tiên hiển thị trạng thái đặc biệt trước
        if ("PENDING_PAYMENT".equals(status) && isWinner) {
            text = "  ⏳ CHỜ THANH TOÁN  "; color = "#FF9800"; 
        } else if ("CANCELLED".equals(status) && isWinner) {
            text = "  ⛔ ĐÃ HỦY  "; color = "#555";
        } else {
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

    // ─── Payment dialog ───────────────────────────────────────────────────────

    /**
     * Mở dialog đếm ngược 10 phút với nút Thanh Toán và Hủy.
     */
    private void openPaymentDialog(Map<String, Object> auction) {
        int auctionId   = ((Number) auction.get("auctionId")).intValue();
        String itemName = (String) auction.getOrDefault("itemName", "Sản phẩm");
        double bid      = auction.get("currentHighestBid") instanceof Number
                ? ((Number) auction.get("currentHighestBid")).doubleValue() : 0;
        String deadlineStr = (String) auction.get("paymentDeadline");

        // Tính số giây còn lại
        long secondsLeft = 600; // fallback 10 phút
        if (deadlineStr != null && !deadlineStr.isEmpty()) {
            try {
                LocalDateTime deadline = LocalDateTime.parse(
                        deadlineStr.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), deadline);
                if (secondsLeft < 0) secondsLeft = 0;
            } catch (Exception ignored) {}
        }
        final long[] remaining = {secondsLeft};

        // ── Xây dựng nội dung dialog ──
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Thanh toán sản phẩm");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setStyle("-fx-background-color: #1a1a1a;");

        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 30; -fx-background-color: #1a1a1a;");
        content.setAlignment(Pos.CENTER);
        content.setPrefWidth(480);

        // Ảnh sản phẩm
        String rawImg = (String) auction.get("itemImagePath");
        if (rawImg != null && !rawImg.trim().isEmpty()) {
            String firstUrl = rawImg.split(",")[0].trim();
            if (firstUrl.startsWith("http")) {
                try {
                    ImageView imgView = new ImageView(new Image(firstUrl, 440, 220, true, true, true));
                    imgView.setFitWidth(440);
                    imgView.setFitHeight(220);
                    imgView.setPreserveRatio(true);
                    imgView.setStyle("-fx-background-radius: 10;");
                    content.getChildren().add(imgView);
                } catch (Exception ignored) {}
            }
        }

        // Tên sản phẩm
        Label nameLbl = new Label(itemName);
        nameLbl.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        nameLbl.setWrapText(true);

        // Giá
        Label priceLbl = new Label(String.format("Số tiền cần thanh toán:  %,.0f ₫", bid));
        priceLbl.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 18px; -fx-font-weight: bold;");

        // Đếm ngược
        Label countdownLbl = new Label();
        countdownLbl.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 22px; -fx-font-weight: bold;");
        updateCountdownLabel(countdownLbl, remaining[0]);

        Label warnLbl = new Label("⚠️  Chưa thanh toán trong thời hạn sẽ tự động hủy");
        warnLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");

        // Hai nút hành động
        Button btnPay    = new Button("💳  THANH TOÁN NGAY");
        Button btnCancel = new Button("✕  Hủy bỏ");

        btnPay.setStyle(
            "-fx-background-color: #1976D2; -fx-text-fill: white;" +
            "-fx-font-size: 16px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8; -fx-cursor: hand;" +
            "-fx-padding: 14 32 14 32;");
        btnCancel.setStyle(
            "-fx-background-color: #880000; -fx-text-fill: white;" +
            "-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8; -fx-cursor: hand;" +
            "-fx-padding: 12 24 12 24;");

        HBox btnRow = new HBox(20, btnPay, btnCancel);
        btnRow.setAlignment(Pos.CENTER);

        content.getChildren().addAll(nameLbl, priceLbl, countdownLbl, warnLbl, btnRow);
        dialog.getDialogPane().setContent(content);
        // Xóa nút mặc định của dialog
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);

        // ── Timer đếm ngược ──
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        final boolean[] dialogClosed = {false};

        ScheduledFuture<?>[] timerRef = new ScheduledFuture<?>[1];
        timerRef[0] = scheduler.scheduleAtFixedRate(() -> {
            remaining[0]--;
            Platform.runLater(() -> updateCountdownLabel(countdownLbl, remaining[0]));
            if (remaining[0] <= 0) {
                timerRef[0].cancel(false);
                scheduler.shutdown();
                Platform.runLater(() -> {
                    if (!dialogClosed[0]) {
                        dialogClosed[0] = true;
                        dialog.close();
                        showAlert(Alert.AlertType.WARNING, "Hết hạn thanh toán",
                                "Bạn đã không thanh toán trong thời hạn.\nSản phẩm đã bị hủy.");
                        loadMyProducts(); // Reload để cập nhật status
                    }
                });
            }
        }, 1, 1, TimeUnit.SECONDS);

        // ── Nút Thanh Toán ──
        btnPay.setOnAction(e -> {
            timerRef[0].cancel(false);
            scheduler.shutdown();
            dialogClosed[0] = true;
            dialog.close();

            Map<String, Object> data = new HashMap<>();
            data.put("auctionId", auctionId);
            data.put("userId", UserSession.getInstance().getUserId());
            Response response = ServerConnection.getInstance().send("CONFIRM_PAYMENT", data);

            if (response.isSuccess()) {
                showAlert(Alert.AlertType.INFORMATION, "Thanh toán thành công", response.getMessage());
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", response.getMessage());
            }
            loadMyProducts();
        });

        // ── Nút Hủy ──
        btnCancel.setOnAction(e -> {
            showConfirm(
                "Xác nhận hủy",
                "Bạn có chắc muốn hủy?\nSản phẩm sẽ không được bàn giao.",
                () -> {
                    timerRef[0].cancel(false);
                    scheduler.shutdown();
                    dialogClosed[0] = true;
                    dialog.close();

                    Map<String, Object> data = new HashMap<>();
                    data.put("auctionId", auctionId);
                    data.put("userId", UserSession.getInstance().getUserId());
                    Response response = ServerConnection.getInstance().send("CANCEL_PAYMENT", data);

                    if (response.isSuccess()) {
                        showAlert(Alert.AlertType.INFORMATION, "Đã hủy", response.getMessage());
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", response.getMessage());
                    }
                    loadMyProducts();
                }
            );
        });

        // Dọn dẹp timer khi dialog bị đóng bằng cách khác
        dialog.setOnHidden(ev -> {
            dialogClosed[0] = true;
            if (!scheduler.isShutdown()) scheduler.shutdownNow();
        });

        dialog.showAndWait();
    }

    /** Cập nhật label đếm ngược từ số giây */
    private void updateCountdownLabel(Label lbl, long secondsLeft) {
        if (secondsLeft <= 0) {
            lbl.setText("⏰  00:00  —  Đã hết hạn!");
            lbl.setStyle("-fx-text-fill: #CC0000; -fx-font-size: 22px; -fx-font-weight: bold;");
            return;
        }
        long mins = secondsLeft / 60;
        long secs = secondsLeft % 60;
        String timeStr = String.format("⏳  %02d:%02d  còn lại", mins, secs);
        lbl.setText(timeStr);
        // Đổi màu sang đỏ khi còn dưới 2 phút
        if (secondsLeft <= 120) {
            lbl.setStyle("-fx-text-fill: #CC0000; -fx-font-size: 22px; -fx-font-weight: bold;");
        } else {
            lbl.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 22px; -fx-font-weight: bold;");
        }
    }

    // ─── Custom overlay notifications (không dùng Stage mới) ────────────────

    /**
     * Hiển thị thông báo dạng overlay modal ngay trên scene hiện tại.
     * Không tạo Stage mới nên không bao giờ bị mất sau primaryStage.
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        String accentColor, iconText;
        switch (type) {
            case ERROR:        accentColor = "#CC0000"; iconText = "✕"; break;
            case WARNING:      accentColor = "#FF9800"; iconText = "⚠"; break;
            case CONFIRMATION: accentColor = "#1976D2"; iconText = "?"; break;
            default:           accentColor = "#4CAF50"; iconText = "✓"; break;
        }
        showOverlay(buildAlertBox(accentColor, iconText, title, message, null));
    }

    /**
     * Hiển thị xác nhận hai nút. onConfirm chạy nếu user chọn "Có".
     */
    private void showConfirm(String title, String message, Runnable onConfirm) {
        showOverlay(buildAlertBox("#CC0000", "⚠", title, message, onConfirm));
    }

    /** Inject overlay backdrop + card vào rootPane. */
    private void showOverlay(VBox card) {
        // Backdrop mờ
        StackPane backdrop = new StackPane(card);
        backdrop.setStyle("-fx-background-color: rgba(0,0,0,0.65);");
        backdrop.setAlignment(Pos.CENTER);

        // Đóng khi click ra ngoài card
        backdrop.setOnMouseClicked(e -> {
            if (e.getTarget() == backdrop) rootPane.getChildren().remove(backdrop);
        });
        card.setOnMouseClicked(javafx.event.Event::consume);

        rootPane.getChildren().add(backdrop);
    }

    /**
     * Tạo card thông báo.
     * Nếu onConfirm != null → hiện hai nút "Không" / "Có, xác nhận" (confirm mode).
     * Nếu onConfirm == null → chỉ một nút OK (alert mode).
     */
    private VBox buildAlertBox(String accentColor, String iconText,
                                String title, String message, Runnable onConfirm) {
        VBox card = new VBox(0);
        card.setPrefWidth(420);
        card.setMaxWidth(420);
        card.setStyle(
            "-fx-background-color: #1a1a1a;" +
            "-fx-background-radius: 14;" +
            "-fx-border-radius: 14;" +
            "-fx-border-color: #2e2e2e;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 40, 0, 0, 10);"
        );

        // Stripe màu
        Region stripe = new Region();
        stripe.setPrefHeight(4);
        stripe.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 14 14 0 0;");

        // Body
        HBox body = new HBox(18);
        body.setAlignment(Pos.CENTER_LEFT);
        body.setStyle("-fx-padding: 28 28 22 28;");

        Label iconLbl = new Label(iconText);
        iconLbl.setMinSize(48, 48);
        iconLbl.setPrefSize(48, 48);
        iconLbl.setAlignment(Pos.CENTER);
        iconLbl.setStyle(
            "-fx-background-color: " + accentColor + "1a;" +
            "-fx-text-fill: " + accentColor + ";" +
            "-fx-font-size: 22px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 24;"
        );

        VBox textBlock = new VBox(7);
        HBox.setHgrow(textBlock, Priority.ALWAYS);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        titleLbl.setWrapText(true);

        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-text-fill: #999; -fx-font-size: 13px; -fx-line-spacing: 3;");
        msgLbl.setWrapText(true);
        msgLbl.setMaxWidth(310);

        textBlock.getChildren().addAll(titleLbl, msgLbl);
        body.getChildren().addAll(iconLbl, textBlock);

        // Divider
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #252525;");

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-padding: 14 24 18 24;");

        if (onConfirm == null) {
            // Alert mode: chỉ nút OK
            Button btnOk = makeBtn("OK", accentColor, true);
            btnOk.setOnAction(e -> {
                StackPane backdrop = (StackPane) card.getParent();
                rootPane.getChildren().remove(backdrop);
            });
            footer.getChildren().add(btnOk);
        } else {
            // Confirm mode: Không + Có
            Button btnNo = makeBtn("Không", "#2e2e2e", false);
            btnNo.setStyle(btnNo.getStyle() + "-fx-text-fill: #888;");
            Button btnYes = makeBtn("Có, xác nhận", accentColor, true);

            btnNo.setOnAction(e -> {
                StackPane backdrop = (StackPane) card.getParent();
                rootPane.getChildren().remove(backdrop);
            });
            btnYes.setOnAction(e -> {
                StackPane backdrop = (StackPane) card.getParent();
                rootPane.getChildren().remove(backdrop);
                onConfirm.run();
            });
            footer.getChildren().addAll(btnNo, btnYes);
        }

        card.getChildren().addAll(stripe, body, divider, footer);
        return card;
    }

    private Button makeBtn(String text, String bgColor, boolean bold) {
        Button btn = new Button(text);
        btn.setPrefHeight(38);
        btn.setMinWidth(90);
        btn.setStyle(
            "-fx-background-color: " + bgColor + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13px;" +
            (bold ? "-fx-font-weight: bold;" : "") +
            "-fx-background-radius: 7;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 0 20 0 20;"
        );
        return btn;
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    @FXML
    private void goBack() {
        Main.changeScene("/view/home.fxml");
    }
}
