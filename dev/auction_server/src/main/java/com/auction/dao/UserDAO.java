package com.auction.dao;

import com.auction.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserDAO {

    // Khởi tạo Logger
    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    // 1. Hàm lưu User vào Database
    public void insertUser(User user) {
        String sql = "INSERT INTO users (username, account_name, password_hash, role, rating, auto_bid_enabled, max_auto_bid_amount, auto_bid_increment) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());

            if (user instanceof Seller) {
                stmt.setString(4, "SELLER");
                stmt.setDouble(5, ((Seller) user).getRating());
                stmt.setNull(6, Types.BOOLEAN);
                stmt.setDouble(7, 0);
                stmt.setDouble(8, 0);
            } else if (user instanceof Bidder) {
                stmt.setString(4, "BIDDER");
                stmt.setDouble(5, 0);
                stmt.setBoolean(6, ((Bidder) user).isAutoBidEnabled());
                stmt.setDouble(7, ((Bidder) user).getMaxAutoBidAmount());
                stmt.setDouble(8, ((Bidder) user).getAutoBidIncrement());
            } else {
                stmt.setString(4, "ADMIN");
                stmt.setNull(5, Types.DOUBLE);
                stmt.setNull(6, Types.BOOLEAN);
                stmt.setDouble(7, 0);
                stmt.setDouble(8, 0);
            }

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }
            // Thay thế System.out bằng Logger
            LOGGER.info(() -> "💾 Đã lưu User '" + user.getUsername() + "' vào Database.");

        } catch (SQLException e) {
            // Thay thế printStackTrace bằng Logger
            LOGGER.log(Level.SEVERE, "❌ Lỗi khi lưu User vào Database", e);
        }
    }

    // 2. Hàm lấy tất cả User từ Database
    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        // ĐÃ SỬA: Thay SELECT * bằng danh sách cột cụ thể
        String sql = "SELECT id, username, account_name, password_hash, role, rating, auto_bid_enabled, max_auto_bid_amount, auto_bid_increment FROM users";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("username");
                String email = rs.getString("account_name");
                String pass = rs.getString("password_hash");
                String role = rs.getString("role");

                User u;
                if ("SELLER".equals(role)) {
                    u = new Seller(id, name, email, pass);
                    ((Seller) u).setRating(rs.getDouble("rating"));
                } else if ("BIDDER".equals(role)) {
                    u = new Bidder(id, name, email, pass);
                    ((Bidder) u).setAutoBidEnabled(rs.getBoolean("auto_bid_enabled"));
                    ((Bidder) u).setMaxAutoBidAmount(rs.getDouble("max_auto_bid_amount"));
                    ((Bidder) u).setAutoBidIncrement(rs.getDouble("auto_bid_increment"));
                } else {
                    u = new Admin(id, name, email, pass);
                }
                userList.add(u);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi khi lấy danh sách User", e);
        }
        return userList;
    }
}