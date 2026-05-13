package com.auction.client;

import com.auction.client.network.ServerConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        primaryStage.setTitle("Hệ Thống Đấu Giá");

        // Kết nối tới server khi ứng dụng khởi động
        boolean connected = ServerConnection.getInstance().connect();
        if (!connected) {
            System.err.println("⚠️ Không thể kết nối server. Chạy ở chế độ offline (chỉ UI).");
        }

        // Đóng kết nối khi người dùng tắt cửa sổ
        stage.setOnCloseRequest(event -> ServerConnection.getInstance().disconnect());

        changeScene("/view/home.fxml");
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void changeScene(String fxmlPath) {
        try {
            URL resource = Main.class.getResource(fxmlPath);
            if (resource == null) {
                throw new IOException("Không tìm thấy file FXML tại: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            if (primaryStage.getScene() == null) {
                primaryStage.setScene(new Scene(root));
            } else {
                primaryStage.getScene().setRoot(root);
            }
            primaryStage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("LỖI CẤU TRÚC: Kiểm tra thư mục resources/view xem có file fxml chưa!");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
