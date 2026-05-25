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
                ensurePhoneColumn(connection);
            }
        } catch (SQLException | ClassNotFoundException e) {
            throw new IllegalStateException("Không thể kết nối DB, dừng hệ thống.", e);
        }
        return connection;
    }

    /**
     * Tự động thêm cột phone vào bảng users nếu chưa có.
     */
    private static void ensurePhoneColumn(Connection conn) {
        try (java.sql.ResultSet rs = conn.getMetaData().getColumns(null, null, "users", "phone")) {
            if (!rs.next()) {
                try (java.sql.Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN phone VARCHAR(20) DEFAULT ''");
                    LOGGER.info("✅ Đã thêm cột 'phone' vào bảng users.");
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("⚠️ Không thể kiểm tra/thêm cột phone: " + e.getMessage());
        }

        // Thêm cột display_name (họ tên hiển thị) nếu chưa có
        try (java.sql.ResultSet rs = conn.getMetaData().getColumns(null, null, "users", "display_name")) {
            if (!rs.next()) {
                try (java.sql.Statement stmt = conn.createStatement()) {
                    // Khởi tạo display_name = account_name cho user cũ (account_name đang chứa họ tên)
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN display_name VARCHAR(255) DEFAULT ''");
                    stmt.executeUpdate("UPDATE users SET display_name = account_name WHERE display_name = '' OR display_name IS NULL");
                    LOGGER.info("✅ Đã thêm cột 'display_name' và migrate dữ liệu từ account_name.");
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("⚠️ Không thể kiểm tra/thêm cột display_name: " + e.getMessage());
        }
    }
}