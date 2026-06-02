package com.auction.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Quản lý Connection Pool bằng HikariCP.
 *
 * Thay vì mỗi lần cần DB lại tạo mới một kết nối TCP (tốn ~100-300ms),
 * HikariCP duy trì sẵn một pool các kết nối đã được thiết lập.
 * Khi cần, lấy ra dùng; khi xong, trả lại pool — không đóng thật.
 *
 * Kết quả: latency giảm ~100×, server chịu tải tốt hơn khi nhiều client đồng thời.
 */
public class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    // Pool duy nhất cho toàn server — khởi tạo 1 lần duy nhất khi class được load
    private static final HikariDataSource DATA_SOURCE;

    static {
        Properties props = new Properties();
        try (InputStream is = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream("server.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            LOGGER.info("Không tìm thấy server.properties, dùng cấu hình mặc định.");
        }

        String url      = props.getProperty("db.url",
                "jdbc:mysql://auction-system-tungletrong347-aad4.c.aivencloud.com:19823/defaultdb" +
                "?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String user     = props.getProperty("db.user",     "avnadmin");
        String password = props.getProperty("db.password", "AVNS_y9I15RDAzV-XLdCZFjC");

        HikariConfig config = new HikariConfig();

        // ── Kết nối cơ bản ──
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // ── Kích thước pool ──
        // minimumIdle: số kết nối luôn giữ sẵn dù không có request
        // maximumPoolSize: trần trên — với server nhỏ 10 là đủ
        config.setMinimumIdle(3);
        config.setMaximumPoolSize(10);

        // ── Timeout & keepalive ──
        // connectionTimeout: thời gian tối đa chờ lấy connection từ pool (ms)
        config.setConnectionTimeout(30_000);          // 30 giây
        // idleTimeout: kết nối idle bị đóng sau bao lâu (ms) — giải phóng tài nguyên Aiven
        config.setIdleTimeout(600_000);               // 10 phút
        // maxLifetime: tuổi thọ tối đa của 1 connection — nên nhỏ hơn wait_timeout của MySQL
        config.setMaxLifetime(1_800_000);             // 30 phút
        // keepaliveTime: định kỳ ping DB để kết nối không bị Aiven cắt sau idle
        config.setKeepaliveTime(60_000);              // ping mỗi 1 phút

        // ── Kiểm tra connection còn sống trước khi cấp ra ──
        config.setConnectionTestQuery("SELECT 1");

        // ── Tên pool hiển thị trong log ──
        config.setPoolName("AuctionPool");

        DATA_SOURCE = new HikariDataSource(config);
        LOGGER.info("✅ HikariCP pool khởi động thành công (pool size: 3–10).");

        // Tự động migrate schema nếu thiếu cột
        ensureSchema();
    }

    private DatabaseConnection() {}

    /**
     * Lấy một Connection từ pool.
     * Dùng trong try-with-resources: conn sẽ được trả về pool khi block kết thúc,
     * không phải đóng kết nối thật sự.
     */
    public static Connection getConnection() {
        try {
            return DATA_SOURCE.getConnection();
        } catch (SQLException e) {
            throw new IllegalStateException("❌ Lỗi lấy connection từ pool!", e);
        }
    }

    /**
     * Đóng toàn bộ pool khi server tắt.
     * Gọi trong shutdown hook của Main.
     */
    public static void shutdown() {
        if (DATA_SOURCE != null && !DATA_SOURCE.isClosed()) {
            DATA_SOURCE.close();
            LOGGER.info("🔌 HikariCP pool đã đóng.");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Schema migration tự động — thêm cột nếu chưa tồn tại
    // ──────────────────────────────────────────────────────────────────────────

    private static void ensureSchema() {
        try (Connection conn = DATA_SOURCE.getConnection()) {
            ensureColumn(conn, "users",    "phone",            "VARCHAR(20) DEFAULT ''");
            ensureColumn(conn, "users",    "display_name",     "VARCHAR(255) DEFAULT ''");
            ensureColumn(conn, "auctions", "payment_deadline", "DATETIME DEFAULT NULL");

            // Backfill display_name từ account_name cho user cũ
            try (java.sql.Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "UPDATE users SET display_name = account_name " +
                    "WHERE (display_name IS NULL OR display_name = '') AND account_name IS NOT NULL"
                );
            } catch (SQLException ignored) {}

        } catch (SQLException e) {
            LOGGER.warning("⚠️ Không thể chạy schema migration: " + e.getMessage());
        }
    }

    /** Thêm cột vào bảng nếu chưa có. */
    private static void ensureColumn(Connection conn, String table, String column, String definition) {
        try (java.sql.ResultSet rs = conn.getMetaData().getColumns(null, null, table, column)) {
            if (!rs.next()) {
                try (java.sql.Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                    LOGGER.info("✅ Đã thêm cột '" + column + "' vào bảng " + table + ".");
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("⚠️ Không thể thêm cột " + column + " vào " + table + ": " + e.getMessage());
        }
    }
}
