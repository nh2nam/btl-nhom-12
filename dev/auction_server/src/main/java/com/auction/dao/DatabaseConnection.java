package com.auction.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

public class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    private static final String URL;
    private static final String USER;
    private static final String DB_SECRET;

    // Đọc config từ server.properties khi class được load
    static {
        Properties props = new Properties();
        try (InputStream is = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream("server.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            LOGGER.info("Không tìm thấy server.properties, sử dụng cấu hình mặc định (localhost).");
        }
        //https://console.aiven.io/account/a5ac8786cd0e/project/tungletrong347-aad4/services/auction-system/overview
        // Đổi IP cứng thành localhost để Server luôn tự kết nối được với DB của chính nó
        URL = props.getProperty("db.url", "jdbc:mysql://auction-system-tungletrong347-aad4.c.aivencloud.com:19823/defaultdb?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        USER      = props.getProperty("db.user",     "avnadmin");
        DB_SECRET = props.getProperty("db.password", "AVNS_y9I15RDAzV-XLdCZFjC");
    }

    private static Connection connection;

    private DatabaseConnection() {}

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, DB_SECRET);
                LOGGER.info("🔗 Đã kết nối tới Database thành công!");
            }
        } catch (SQLException | ClassNotFoundException e) {
            throw new IllegalStateException("Không thể kết nối DB, dừng hệ thống.", e);
        }
        return connection;
    }
}